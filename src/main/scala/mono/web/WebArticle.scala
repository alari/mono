package mono.web

import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.{ NonEmptyList, Validated }
import cats.free.Free
import cats.~>
import monix.eval.Task
import mono.article.{ Article, ArticleOps }
import mono.author.{ Author, AuthorOps }

import scala.language.higherKinds
import monix.execution.Scheduler.Implicits.global
import mono.env.EnvOps
import play.twirl.api.Html
import cats.implicits._
import mono.alias.{ Alias, AliasOps, AliasPointer }
import mono.image.{ Image, ImageOps }

class WebArticle[F[_]](implicit A: ArticleOps[F], Au: AuthorOps[F], As: AliasOps[F], E: EnvOps[F], Im: ImageOps[F]) extends Web[F] {

  import WebArticle._
  import WebTokenCheck._

  override def route(implicit i: F ~> Task): Route = path(LongNumber) { articleId ⇒
    // TODO: check permissions
    articleHtml[F](articleId)
  } ~ path("edit" / LongNumber){ articleId ⇒
    checkToken[F](s"edit/" + articleId).apply { author ⇒
      get {
        editArticleHtml[F](articleId)
      } ~ (post & entity(as[FormData])) { d ⇒
        val fields = d.fields.toMap
        update[F](articleId, fields, author).flatMap {
          case Validated.Valid((a, al)) ⇒
            A.getText(articleId).map(text ⇒ html.editArticle(a, text, al.map(_.id), Map.empty))

          case Validated.Invalid(errs) ⇒
            for {
              article ← A.getById(articleId)
              text ← A.getText(articleId)
            } yield html.editArticle(article, text, fields.get("alias"), errs.toList.groupBy(_._1).mapValues(_.map(_._2)))

        }
      }
    }
  } ~ (pathEndOrSingleSlash & parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'authorId.as[Long].?, 'q.?)) { (o, l, a, q) ⇒
    articlesHtml(o, l, a, q)
  }

}

object WebArticle {
  def updateText[F[_]](article: Article, text: Option[String], author: Author)(implicit A: ArticleOps[F]): Free[F, Article] =
    text.map(_.trim) match {
      case Some(value) ⇒
        A.getText(article.id).flatMap {
          case `value` ⇒ Free.pure(article)
          case _ ⇒
            A.setText(article.id, value)
              .flatMap(_ ⇒ A.getById(article.id))
        }
      case None ⇒
        Free.pure(article)
    }

  def updateArticle[F[_]](article: Article, fields: Map[String, String], author: Author)(implicit A: ArticleOps[F]): Free[F, Validated[NonEmptyList[(String, String)], Article]] =
    {
      type V[T] = Validated[NonEmptyList[(String, String)], T]

      def readString(name: String): V[String] =
        fields.get(name).map(_.trim).filter(_.nonEmpty)
          .fold[V[String]](Validated.invalidNel(name → "Must be not empty"))(Validated.valid[NonEmptyList[(String, String)], String])

      def readStringOpt(name: String): V[Option[String]] =
        Validated.valid[NonEmptyList[(String, String)], Option[String]](fields.get(name))

      def readInt(name: String): V[Int] =
        readString(name)
          .andThen(s ⇒
            Validated.catchOnly[NumberFormatException](s.toInt).leftMap(e ⇒ NonEmptyList.of(name → e.getMessage)))

      (readString("title") |@| readStringOpt("headline") |@| readInt("publishedAt")).map { (title, headline, publishedAt) ⇒
        if (article.title != title ||
          article.headline != headline ||
          article.publishedAt != publishedAt) A.update(article.id, title, headline, publishedAt)
        else Free.pure[F, Article](article)
      }.sequence
    }

  def update[F[_]](articleId: Long, fields: Map[String, String], author: Author)(implicit A: ArticleOps[F], As: AliasOps[F]): Free[F, Validated[NonEmptyList[(String, String)], (Article, Option[Alias])]] =
    for {
      article ← A.getById(articleId)
      aText ← updateText(article, fields.get("text"), author)
      aUp ← updateArticle(aText, fields, author)
      al ← fields.get("alias").map(_.trim).filter(_.nonEmpty) match {
        case Some(alias) if aUp.isValid ⇒ As.tryPointTo(alias, article, force = true)
        case _                          ⇒ As.findAlias(article)
      }
    } yield aUp.map(_ → al)

  def editArticleHtml[F[_]](articleId: Long)(implicit A: ArticleOps[F], As: AliasOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
      alias ← As.findAlias(article)
    } yield html.editArticle(article, text, alias.map(_.id), Map.empty)

  def articleHtml[F[_]](articleId: Long)(implicit A: ArticleOps[F], Au: AuthorOps[F], Im: ImageOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
      author ← Au.getById(article.authorId)
      cover ← article.coverId match {
        case Some(coverId) ⇒ Im.find(coverId)
        case None          ⇒ Free.pure[F, Option[Image]](None)
      }
    } yield html.article(article, text, author, cover)

  def articlesHtml[F[_]](offset: Int, limit: Int, authorId: Option[Long], q: Option[String])(implicit A: ArticleOps[F], Au: AuthorOps[F]): Free[F, Html] =
    for {
      articles ← A.fetch(authorId, q, offset, limit)
      authors ← Au.getByIds(articles.values.map(_.authorId).toSet)
    } yield html.articles(articles, authors)
}
