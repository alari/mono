package mono.bot

import java.time.Instant

import cats.data.Coproduct
import cats.free.Free
import mono.Interpret
import mono.alias.{ Alias, AliasOps }
import mono.article.ArticleOps
import mono.author.AuthorOps

class BotScript()(implicit
  B: BotOps[BotScript.Op],
                  A:  ArticleOps[BotScript.Op],
                  Au: AuthorOps[BotScript.Op],
                  Ao: AliasOps[BotScript.Op]) {

  import BotScript.{ Op, Scenario }
  import BotState._

  private val newTitleR = "/new(@[^ ]+)? (.+)".r

  val createArticle: Scenario = {
    case (Idle, Plain(newTitleR(_, title), m)) ⇒
      for {
        _ ← B.reply("Give Me Description", m, forceReply = true)
      } yield CreateArticleWaitDescription(title)

    case (CreateArticleWaitDescription(title), Plain(text, m)) ⇒
      for {
        au ← Au.ensureTelegram(m.chat.id, m.chat.title.getOrElse("??? " + m.chat.id))
        _ ← m.chat.alias.fold(Free.pure[Op, Option[Alias]](None))(alias ⇒ Ao.tryPointAuthorTo(alias, au.id))
        a ← A.create(au.id, title, Some(text).map(_.trim).filter(_.nonEmpty), Instant.now())
        al ← Ao.tryPointArticleTo(a.title, a.id)
        _ ← B.reply(s"Created: $a (alias: $al)", m)
      } yield Idle
  }

  val fetchArticles: Scenario = {
    case (Idle, m) ⇒
      for {
        a ← A.fetch(None, None, 0, 1)
        _ ← B.say("Fetched: " + a, m.meta)
        _ ← if (a.count > 1) B.say("we have more", m.meta) else B.say("That's all", m.meta)
      } yield if (a.count > 1)
        Fetching(0, 1, a.count)
      else Idle

    case (Fetching(o, l, _), m) ⇒
      for {
        a ← A.fetch(None, None, o + l, l)
        _ ← B.say("Fetched: " + a, m.meta.chat.id)
        _ ← if (a.count > o + l + l) B.say(s"we have more", m.meta) else B.say("That's all", m.meta)
      } yield if (a.count > o + l + l)
        Fetching(o + l, l, a.count)
      else Idle
  }

  val scenario: Scenario = createArticle orElse fetchArticles

}

object BotScript {

  type Op[A] = Coproduct[BotOp, Interpret.Op, A]
  type Scenario = PartialFunction[(BotState, Incoming), Free[Op, BotState]]

  def apply()(implicit
    B: BotOps[BotScript.Op],
              A:  ArticleOps[BotScript.Op],
              Au: AuthorOps[BotScript.Op],
              Ao: AliasOps[BotScript.Op]): (BotState, Incoming) ⇒ Free[Op, BotState] =
    (state, in) ⇒
      new BotScript().scenario.applyOrElse((state, in), (sm: (BotState, Incoming)) ⇒ for {
        _ ← B.reply(s"Unknown command", sm._2.meta)
      } yield state)

}