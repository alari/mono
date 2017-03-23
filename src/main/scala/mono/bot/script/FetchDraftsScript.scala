package mono.bot.script

import mono.article.ArticleOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.{ BotOps, BotScript, Command }
import ArticleScript.showArticleContext

class FetchDraftsScript(implicit
  B: BotOps[BotScript.Op],
                        A: ArticleOps[BotScript.Op]) extends Script {
  private val draftR = s"draft([0-9]+)".r

  override val scenario: Scenario = {
    case (state, Command("drafts", _, m)) ⇒
      for {
        drafts ← A.fetchDrafts(m.chat.id, 0, 100)

        reply = if (drafts.values.isEmpty) "Нет черновиков. Создайте с /new"
        else drafts.values.map(a ⇒ s"/draft${a.id} ${a.title}").mkString("\n")

        _ ← B.reply(reply, m)
      } yield state

    case (state, Command(draftR(id), _, m)) ⇒
      A.getById(id.toLong).flatMap {
        case a if m.chat.id == a.authorId ⇒
          showArticleContext(a, m)

        case _ ⇒
          for {
            _ ← B.say("Not found", m)
          } yield state
      }
  }
}

object FetchDraftsScript {
  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new FetchDraftsScript
}