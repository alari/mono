package mono.bot.script

import cats.free.Free
import mono.article.{ Article, ArticleOps }
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.BotState.ArticleContext
import mono.bot._

class ArticleScript(implicit
  B: BotOps[BotScript.Op],
                    A: ArticleOps[BotScript.Op]) extends Script {
  override val scenario: Scenario = {
    case (ArticleContext(id, _), Plain(text, m)) ⇒
      for {
        _ ← B.reply("Got in context: " + text, m)
      } yield ArticleContext(id, None)
  }
}

object ArticleScript {
  def showArticleContext(article: Article, meta: Incoming.Meta)(implicit B: BotOps[BotScript.Op]): Free[BotScript.Op, BotState] =
    for {
      _ ← B.say("In context of: " + article.id, meta)
    } yield ArticleContext(article.id)

  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new ArticleScript
}
