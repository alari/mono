package mono.bot.script

import mono.article.ArticleOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.{ BotOps, BotScript }
import mono.bot.BotState.{ FetchingArticles, Idle }

class FetchArticlesScript(implicit
  B: BotOps[BotScript.Op],
                          A: ArticleOps[BotScript.Op]) extends Script {

  override val scenario: Scenario = {
    case (Idle, m) ⇒
      for {
        a ← A.fetch(None, None, 0, 1)
        _ ← B.say("Fetched: " + a, m.meta)
        _ ← if (a.count > 1) B.say("we have more", m.meta) else B.say("That's all", m.meta)
      } yield if (a.count > 1)
        FetchingArticles(0, 1, a.count)
      else Idle

    case (FetchingArticles(o, l, _), m) ⇒
      for {
        a ← A.fetch(None, None, o + l, l)
        _ ← B.say("Fetched: " + a, m.meta.chat.id)
        _ ← if (a.count > o + l + l) B.say(s"we have more", m.meta) else B.say("That's all", m.meta)
      } yield if (a.count > o + l + l)
        FetchingArticles(o + l, l, a.count)
      else Idle
  }

}

object FetchArticlesScript {
  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new FetchArticlesScript
}