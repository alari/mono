package mono.bot

import cats.data.Coproduct
import cats.free.Free
import cats.syntax.monoid._
import mono.Interpret
import mono.alias.AliasOps
import mono.article.ArticleOps
import mono.author.AuthorOps
import mono.bot.script.Script._
import mono.bot.script._
import mono.env.EnvOps

object BotScript {

  type Op[A] = Coproduct[BotOp, Interpret.Op, A]
  type Scenario = PartialFunction[(BotState, Incoming), Free[Op, BotState]]

  def apply()(implicit
    B: BotOps[BotScript.Op],
              A:  ArticleOps[BotScript.Op],
              Au: AuthorOps[BotScript.Op],
              Ao: AliasOps[BotScript.Op],
              E:  EnvOps[BotScript.Op]): (BotState, Incoming) ⇒ Free[Op, BotState] = {

    val script: Script =
      NewArticleScript() |+| FetchDraftsScript() |+| FetchArticlesScript() |+| ArticleScript() |+| HelpScript()

    (state, in) ⇒
      script.scenario.applyOrElse((state, in), (sm: (BotState, Incoming)) ⇒ for {
        _ ← B.reply(s"Unknown command", sm._2.meta)
      } yield state)
  }

}