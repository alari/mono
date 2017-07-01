package mono.bot.script

import cats.free.Free
import mono.alias.AliasPointer.Person
import mono.alias.{ AliasOps, AliasPointer }
import mono.article.{ ArticleOps, Articles }
import mono.person.PersonOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot._
import mono.bot.BotState.{ FetchingArticles, Idle }
import mono.env.EnvOps

class FetchArticlesScript(implicit
  B: BotOps[BotScript.Op],
                          A: ArticleOps[BotScript.Op]) extends Script {

  import FetchArticlesScript._

  override val scenario: Scenario = {
    case (_, Command("fetch", _, meta)) ⇒
      fetch(None, None, 0, 1, meta)

    case (FetchingArticles(o, l, _), m) ⇒
      fetch(None, None, o + l, l, m.meta)
  }

}

object FetchArticlesScript {
  // TODO: сделать красиво
  def fetch(authorId: Option[Long], q: Option[String], offset: Int, limit: Int, m: Incoming.Meta)(implicit
    B: BotOps[BotScript.Op],
                                                                                                  Au: PersonOps[BotScript.Op],
                                                                                                  As: AliasOps[BotScript.Op],
                                                                                                  A:  ArticleOps[BotScript.Op],
                                                                                                  E:  EnvOps[BotScript.Op]): Free[BotScript.Op, BotState] = for {
    as ← A.fetch(None, None, offset, limit)
    Articles(articles, count) = as
    authors ← Au.getByIds(articles.flatMap(_.authorIds.toList).toSet)
    aliases ← As.findAliases(articles.map(a ⇒ a: AliasPointer): _*)
    host ← E.readHost()

    _ ← B.say(articles.map(a ⇒
      s"""**${a.title}** - _${a.authorIds.toList.map(authors).map(_.name).mkString("_, _")}_\n\t/show${a.id}\t[$host/${aliases.get(a).fold(a.id.toString)(_.id)}]""").mkString("\n\n"), m.chat.id)
    _ ← if (count > offset + limit) B.say(s"Осталось: ${count - offset - limit}", m) else B.say("Всё!", m)
  } yield (if (count > offset + limit)
    FetchingArticles(offset, limit, count)
  else Idle).asInstanceOf[BotState]

  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new FetchArticlesScript
}