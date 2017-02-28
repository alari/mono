package mono.bot

import java.time.Instant

import cats.data.Coproduct
import cats.free.Free
import mono.article.{ ArticleOp, ArticleOps }

class BotScript()(implicit B: BotOps[BotScript.Op], A: ArticleOps[BotScript.Op]) {

  import BotScript.Scenario
  import BotState._

  private val newTitleR = "/new (.+)".r

  val createArticle: Scenario = {
    case (Idle, Plain(newTitleR(title), chatId)) ⇒
      for {
        _ ← B.say("Give Me Description", chatId)
      } yield CreateArticleWaitDescription(title)

    case (CreateArticleWaitDescription(title), Plain(text, chatId)) ⇒
      for {
        a ← A.create("user", title, Some(text).map(_.trim).filter(_.nonEmpty), Instant.now())
        _ ← B.say("Created: " + a, chatId)
      } yield Idle
  }

  val fetchArticles: Scenario = {
    case (Idle, m) ⇒
      for {
        a ← A.fetch(None, None, 0, 1)
        _ ← B.say("Fetched: " + a, m.chatId)
        _ ← if (a.count > 1) B.say("we have more", m.chatId) else B.say("That's all", m.chatId)
      } yield if (a.count > 1)
        Fetching(0, 1, a.count)
      else Idle

    case (Fetching(o, l, _), m) ⇒
      for {
        a ← A.fetch(None, None, o + l, l)
        _ ← B.say("Fetched: " + a, m.chatId)
        _ ← if (a.count > o + l + l) B.say(s"we have more", m.chatId) else B.say("That's all", m.chatId)
      } yield if (a.count > o + l + l)
        Fetching(o + l, l, a.count)
      else Idle
  }

  val scenario: Scenario = createArticle orElse fetchArticles

}

object BotScript {
  type Op[A] = Coproduct[BotOp, ArticleOp, A]
  type Scenario = PartialFunction[(BotState, Incoming), Free[Op, BotState]]

  def apply()(implicit B: BotOps[BotScript.Op], A: ArticleOps[BotScript.Op]): (BotState, Incoming) ⇒ Free[Op, BotState] =
    (state, in) ⇒
      new BotScript().scenario.applyOrElse((state, in), (sm: (BotState, Incoming)) ⇒ for {
        _ ← B.say(s"Unknown command: `$in`", sm._2.chatId)
      } yield state)

}