package mono.bot.script

import java.time.Instant

import cats.free.Free
import mono.alias.AliasOps
import mono.article.{ Article, ArticleOps }
import mono.author.AuthorOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.BotState.{ ArticleContentContext, ArticleContext, ArticleDescriptionContext, ArticleTitleContext }
import mono.bot._
import mono.env.EnvOps
import pdi.jwt.JwtClaim

import scala.io.Source

/**
 * TODO: загрузка картинки обложки
 */
class ArticleScript(implicit
  B: BotOps[BotScript.Op],
                    A:  ArticleOps[BotScript.Op],
                    As: AliasOps[BotScript.Op],
                    E:  EnvOps[BotScript.Op]) extends Script {

  import ArticleScript._

  val showR = "show([0-9]+)".r

  override val scenario: Scenario = {
    case (ArticleContext(id), Plain(`Publish`, m)) ⇒
      for {
        a ← A.publishDraft(id)
        _ ← As.tryPointTo(a.title, a, force = false)
        _ ← B.reply("Опубликован", m)
      } yield ArticleContext(id)

    case (ArticleContext(id), Plain(`Hide`, m)) ⇒
      for {
        _ ← A.draftArticle(id)
        _ ← B.reply("Скрыт", m)
      } yield ArticleContext(id)

    case (ArticleContext(id), Plain(`Show`, m)) ⇒
      showArticle(id, m)

    case (ArticleContext(id), Plain(`EditTitle`, m)) ⇒
      for {
        a ← A.getById(id)
        _ ← B.reply(s"Введите заголовок, сейчас `${a.title}`", m, forceReply = true)
      } yield ArticleTitleContext(id)

    case (ArticleTitleContext(id), Plain(text, m)) ⇒
      for {
        a ← A.setTitle(id, text)
        _ ← B.reply("Сохранили заголовок", m)
        s ← showArticleContext(a, m)
      } yield s

    case (ArticleContext(id), Plain(EditHeadline, m)) ⇒
      for {
        a ← A.getById(id)
        _ ← B.reply(s"Введите аннотацию\n${a.headline.getOrElse("")}", m, forceReply = true)
      } yield ArticleDescriptionContext(id)

    case (ArticleDescriptionContext(id), Plain(text, m)) ⇒
      for {
        a ← A.setHeadline(id, Some(text).map(_.trim).filter(_.nonEmpty))
        _ ← B.reply("Сохранили аннотацию", m)
        s ← showArticleContext(a, m)
      } yield s

    case (ArticleContext(id), Plain(`EditContent`, m)) ⇒
      for {
        _ ← B.reply("Введите содержимое", m)
      } yield ArticleContentContext(id)

    case (ArticleContentContext(id), Plain(text, m)) ⇒
      for {
        _ ← A.setText(id, text)
        a ← A.getById(id)
        _ ← B.reply("Сохранили текст", m)
        s ← showArticleContext(a, m)
      } yield s

    case (ArticleContentContext(id), File(fileId, Some("text/plain"), _, _, m)) ⇒
      for {
        read ← readTextFile(fileId)
        (changeTitle, text) = read
        _ ← A.setText(id, text)
        a ← changeTitle.fold(A.getById(id))(title ⇒ A.setTitle(id, title))
        _ ← B.reply("Сохранили текст", m)
        s ← showArticleContext(a, m)
      } yield s

    case (state, Command(showR(id), _, m)) ⇒
      A.getById(id.toLong).flatMap{ article ⇒
        if (article.draft) {
          for {
            _ ← B.reply("Статья не найдена", m)
          } yield state
        } else showArticle(article.id, m)

      }
  }
}

object ArticleScript {
  val Publish = "Опубликовать"
  val Hide = "Скрыть"
  val Show = "Посмотреть"
  val EditTitle = "Название"
  val EditHeadline = "Аннотация"
  val EditContent = "Текст"

  def readTextFile(fileId: String)(implicit B: BotOps[BotScript.Op]): Free[BotScript.Op, (Option[String], String)] =
    for {
      f ← B.loadFile(fileId)
      lines = Source.fromFile(f.toUri).getLines().toList

      changeTitle = lines.headOption.filter(_.startsWith("# ")).map(_.stripPrefix("# "))

      text = (changeTitle match {
        case Some(t) ⇒ lines.drop(1)
        case None    ⇒ lines
      }).mkString("\n").trim
    } yield (changeTitle, text)

  def showArticle(id: Long, meta: Incoming.Meta)(implicit
    A: ArticleOps[BotScript.Op],
                                                 B: BotOps[BotScript.Op]): Free[BotScript.Op, BotState] =
    for {
      a ← A.getById(id)
      t ← A.getText(id)
      _ ← B.say(s"# ${a.title}\n\n$t", meta)
    } yield ArticleContext(id)

  def showArticleContext(article: Article, meta: Incoming.Meta)(implicit
    As: AliasOps[BotScript.Op],
                                                                Au: AuthorOps[BotScript.Op],
                                                                B:  BotOps[BotScript.Op],
                                                                E:  EnvOps[BotScript.Op]): Free[BotScript.Op, BotState] =
    for {
      url ← As.aliasHref(article, article.id.toString)
      current ← Au.findByTelegramId(meta.chat.id)

      _ ← current match {
        case Some(a) if a.id == article.authorId ⇒
          for {
            host ← E.readHost()
            token ← E.issueToken(
              JwtClaim(
                issuer = Some("telegram"),
                subject = Some(meta.chat.id.toString),
                audience = Some(Set(s"edit/${article.id}")),
                issuedAt = Some(Instant.now().getEpochSecond)
              )
            )
            _ ← B.choose(
              s"${article.title}\n$url\nEdit: $host/edit/${article.id}?token=$token",
              ((if (article.draft) Publish else Hide) :: Show :: Nil) ::
                (EditTitle :: EditHeadline :: EditContent :: Nil) ::
                Nil,
              meta.chat.id
            )
          } yield ()

        case _ ⇒
          B.say(
            s"${article.title}\n$url",
            meta.chat.id
          )
      }

    } yield ArticleContext(article.id)

  def apply()(implicit
    B: BotOps[Op],
              A: ArticleOps[Op]): Script = new ArticleScript
}
