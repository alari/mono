package mono.bot.script

import mono.core.article.ArticleOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.{ BotOps, BotScript, Command }
import ArticleScript.showArticleContext
import mono.core.person.PersonOps

class FetchDraftsScript(implicit
  B: BotOps[BotScript.Op],
                        P: PersonOps[BotScript.Op],
                        A: ArticleOps[BotScript.Op]) extends Script {
  private val draftR = s"draft([0-9]+)".r

  override val scenario: Scenario = {
    case (state, Command("drafts", _, m)) ⇒
      for {
        // TODO: fail if user not found
        person ← P.findByTelegramId(m.chat.id)
        drafts ← A.fetchDrafts(person.map(_.id).get, 0, 100)

        reply = if (drafts.values.isEmpty) "Нет черновиков. Создайте с /new"
        else drafts.values.map(a ⇒ s"/draft${a.id} **${a.title}**").mkString("\n")

        _ ← B.reply(reply, m)
      } yield state

    case (state, Command(draftR(id), _, m)) ⇒
      // TODO: fail if user not found
      P.findByTelegramId(m.chat.id).flatMap { person ⇒
        A.getById(id.toInt)
          .flatMap {
            case a if a.authorIds.toList.exists(person.map(_.id).contains) ⇒
              showArticleContext(a, m)

            case _ ⇒
              for {
                _ ← B.say("Черновик не найден", m)
              } yield state
          }
      }
  }
}

object FetchDraftsScript {
  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new FetchDraftsScript
}