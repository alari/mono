package mono.bot.script

import java.time.Instant

import cats.free.Free
import mono.core.alias.AliasOps
import mono.core.article.{ Article, ArticleOps }
import mono.core.person.PersonOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.BotState.{ ArticleContentContext, ArticleContext, ArticleDescriptionContext, ArticleTitleContext }
import mono.bot._
import mono.core.env.EnvOps
import mono.core.image.ImageOps
import pdi.jwt.JwtClaim

import scala.io.Source

/**
 * TODO: загрузка картинки обложки
 */
class ArticleScript(implicit
  B: BotOps[BotScript.Op],
                    A:  ArticleOps[BotScript.Op],
                    As: AliasOps[BotScript.Op],
                    Au: PersonOps[BotScript.Op],
                    Im: ImageOps[BotScript.Op],
                    E:  EnvOps[BotScript.Op]) extends Script {

  import ArticleScript._

  private val showR = "show([0-9]+)".r

  private val publishR = "article:([0-9]+):publish".r
  private val draftR = "article:([0-9]+):draft".r

  override val scenario: Scenario = {

    case (ArticleContext(id), Image(fileId, caption, m)) ⇒
      for {
        ft ← B.loadFile(fileId)
        a ← Au.ensureTelegram(m.chat.id, m.chat.title.getOrElse(m.chat.id.toString))
        i ← Im.store(a.id, ft, caption)
        a ← A.setCover(id, i.toOption.map(_.id))
        s ← showArticleContext(a, m)
      } yield s

    case (ArticleContentContext(id), File(fileId, Some("text/plain"), _, _, m)) ⇒
      for {
        read ← readTextFile(fileId)
        (_, text) = read
        _ ← A.setText(id, text)
        a ← A.getById(id)
        _ ← B.reply("Сохранили текст", m)
        s ← showArticleContext(a, m)
      } yield s

    case (state, Command(showR(id), _, m)) ⇒
      A.getById(id.toInt).flatMap{ article ⇒
        if (article.isDraft) {
          for {
            _ ← B.reply("Статья не найдена", m)
          } yield state
        } else showArticle(article.id, m)

      }

    case (state, InlineCallback(publishR(id), callbackId, m)) ⇒
      A.getById(id.toInt).flatMap { article ⇒
        for {
          a ← A.publishDraft(article.id)
          _ ← As.tryPointTo(a.title, a, force = false)
          _ ← B.answer(Some("Опубликовано"), callbackId)
          buttons ← ArticleScript.articleContextButtons(a, m)
          _ ← B.inline("", buttons, m.chat.id, Some(m.messageId))
        } yield state
      }

    case (state, InlineCallback(draftR(id), callbackId, m)) ⇒
      A.getById(id.toInt).flatMap { article ⇒
        for {
          a ← A.draftArticle(article.id)
          _ ← As.tryPointTo(a.title, a, force = false)
          _ ← B.answer(Some("Скрыто"), callbackId)
          buttons ← ArticleScript.articleContextButtons(a, m)
          _ ← B.inline("", buttons, m.chat.id, Some(m.messageId))
        } yield state
      }
  }
}

object ArticleScript {
  val Publish = "Опубликовать"
  val Hide = "Скрыть"
  val Show = "Посмотреть"

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

  def showArticle(id: Int, meta: Incoming.Meta)(implicit
    A: ArticleOps[BotScript.Op],
                                                B: BotOps[BotScript.Op]): Free[BotScript.Op, BotState] =
    for {
      a ← A.getById(id)
      t ← A.getText(id)
      _ ← B.say(s"# ${a.title}\n\n$t", meta)
    } yield ArticleContext(id)

  def articleContextButtons(article: Article, meta: Incoming.Meta)(implicit
    As: AliasOps[BotScript.Op],
                                                                   B: BotOps[BotScript.Op],
                                                                   E: EnvOps[BotScript.Op]): Free[BotScript.Op, Seq[Seq[Inline.Button]]] = for {
    url ← As.aliasHref(article, article.id.toString)
    host ← E.readHost()
    token ← E.issueToken(
      JwtClaim(
        issuer = Some("telegram"),
        subject = Some(meta.chat.id.toString),
        audience = Some(Set(s"edit/${article.id}")),
        issuedAt = Some(Instant.now().getEpochSecond)
      )
    )
  } yield Seq(
    Seq(
      Inline.CallbackButton(if (article.isDraft) Publish else Hide, s"article:${article.id}:${if (article.isDraft) "publish" else "draft"}"),
      Inline.UrlButton("Смотреть", url),
      Inline.UrlButton("Редактировать", s"$host/edit/${article.id}?token=$token")
    )
  )

  def showArticleContext(article: Article, meta: Incoming.Meta)(implicit
    As: AliasOps[BotScript.Op],
                                                                Au: PersonOps[BotScript.Op],
                                                                B:  BotOps[BotScript.Op],
                                                                E:  EnvOps[BotScript.Op]): Free[BotScript.Op, BotState] =
    for {
      url ← As.aliasHref(article, article.id.toString)
      current ← Au.findByTelegramId(meta.chat.id)

      _ ← current match {
        case Some(a) if article.authorIds.toList.contains(a.id) ⇒
          for {
            buttons ← articleContextButtons(article, meta)

            _ ← B.inline(
              s"**${article.title}**",
              buttons,
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
