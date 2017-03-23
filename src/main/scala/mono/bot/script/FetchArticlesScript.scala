package mono.bot.script

import cats.free.Free
import mono.article.ArticleOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot._
import mono.bot.BotState.{ FetchingArticles, Idle }

class FetchArticlesScript(implicit
  B: BotOps[BotScript.Op],
                          A: ArticleOps[BotScript.Op]) extends Script {

  import FetchArticlesScript._

  override val scenario: Scenario = {
    case (Idle, m) ⇒
      fetch(None, None, 0, 1, m.meta)

    case (_, Command("fetch", _, meta)) ⇒
      fetch(None, None, 0, 1, meta)

    case (FetchingArticles(o, l, _), m) ⇒
      fetch(None, None, o + l, l, m.meta)
  }

}

object FetchArticlesScript {
  def fetch(authorId: Option[Long], q: Option[String], offset: Int, limit: Int, m: Incoming.Meta)(implicit
    B: BotOps[BotScript.Op],
                                                                                                  A: ArticleOps[BotScript.Op]): Free[BotScript.Op, BotState] = for {
    a ← A.fetch(None, None, offset, limit)
    _ ← B.say("Fetched: " + a, m.chat.id)
    _ ← if (a.count > offset + limit) B.say(s"we have more", m) else B.say("That's all", m)
  } yield if (a.count > offset + limit)
    FetchingArticles(offset, limit, a.count)
  else Idle

  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new FetchArticlesScript
}