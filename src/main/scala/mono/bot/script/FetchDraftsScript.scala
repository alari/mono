package mono.bot.script

import mono.core.article.ArticleOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot._
import ArticleScript.showArticleContext
import cats.free.Free
import mono.core.person.PersonOps

class FetchDraftsScript(implicit
  B: BotOps[BotScript.Op],
                        P: PersonOps[BotScript.Op],
                        A: ArticleOps[BotScript.Op]) extends Script {
  private val draftR = s"show:draft:([0-9]+)".r
  private val listDraftsR = s"show:drafts:([0-9]+)".r

  private val DefaultLimit = 5

  private def showDrafts(state: BotState, offset: Int, limit: Int, m: Incoming.Meta, isReply: Boolean = false): Free[BotScript.Op, BotState] =
    for {
      person ← P.findByTelegramId(m.chat.id)
      drafts ← A.fetchDrafts(person.map(_.id).get, offset, limit)
      buttons = drafts.values.map { a ⇒
        Seq(Inline.CallbackButton(a.title, s"show:draft:${a.id}"))
      } :+ Seq(
        Option(offset - limit).filter(_ >= 0).map(o ⇒ Inline.CallbackButton("←", s"show:drafts:$o")),
        Option(offset + limit).filter(_ < drafts.count).map(o ⇒ Inline.CallbackButton("→", s"show:drafts:$o"))
      ).flatten
      _ ← if (buttons.size > 1) B.inline(s"Ваши черновики. Всего: ${drafts.count}. Создать новый: /new", buttons, m.chat.id, Some(m.messageId).filter(_ ⇒ isReply))
      else B.reply("Нет черновиков. Создайте с /new", m)
    } yield state

  override val scenario: Scenario = {
    case (state, Command("drafts", _, m)) ⇒
      showDrafts(state, 0, DefaultLimit, m)

    case (state, InlineCallback(draftR(id), callbackId, m)) ⇒
      // TODO: fail if user not found
      for {
        person ← P.findByTelegramId(m.chat.id)
        a ← A.getById(id.toInt)
        s ← if (a.authorIds.toList.exists(person.map(_.id).contains)) showArticleContext(a, m)
        else for {
          _ ← B.say("Черновик не найден", m)
        } yield state
        _ ← B.answer(None, callbackId)
      } yield s

    case (state, InlineCallback(listDraftsR(offset), callbackId, m)) ⇒
      for {
        _ ← showDrafts(state, offset.toInt, DefaultLimit, m, isReply = true)
        _ ← B.answer(None, callbackId)
      } yield state
  }
}

object FetchDraftsScript {
  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new FetchDraftsScript
}