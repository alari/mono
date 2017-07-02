package mono.web

import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.{ NonEmptyList, Validated }
import cats.free.Free
import cats.~>
import monix.eval.Task
import mono.core.article.{ Article, ArticleOps }
import mono.core.person.{ Person, PersonOps }

import scala.language.higherKinds
import monix.execution.Scheduler.Implicits.global
import mono.core.env.EnvOps
import play.twirl.api.Html
import cats.implicits._
import mono.core.alias.{ Alias, AliasOps, AliasPointer }
import mono.core.image.{ Image, ImageOps }

class WebArticle[F[_]](implicit A: ArticleOps[F], Au: PersonOps[F], As: AliasOps[F], E: EnvOps[F], Im: ImageOps[F]) extends Web[F] {

  import WebArticle._
  import WebTokenCheck._

  override def route(implicit i: F ~> Task): Route = path(IntNumber) { articleId ⇒
    // TODO: check permissions
    articleHtml[F](articleId)
  } ~ path("edit" / IntNumber){ articleId ⇒
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
  } ~ (pathEndOrSingleSlash & parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'authorId.as[Int].?, 'q.?)) { (o, l, a, q) ⇒
    articlesHtml(o, l, a, q)
  }

}

object WebArticle {
  def updateText[F[_]](article: Article, text: Option[String], author: Person)(implicit A: ArticleOps[F]): Free[F, Article] =
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

  def updateArticle[F[_]](article: Article, fields: Map[String, String], author: Person)(implicit A: ArticleOps[F]): Free[F, Validated[NonEmptyList[(String, String)], Article]] =
    {
      type V[T] = Validated[NonEmptyList[(String, String)], T]

      def readString(name: String): V[String] =
        fields.get(name).map(_.trim).filter(_.nonEmpty)
          .fold[V[String]](Validated.invalidNel(name → "Must be not empty"))(Validated.valid[NonEmptyList[(String, String)], String])

      def readStringOpt(name: String): V[Option[String]] =
        Validated.valid[NonEmptyList[(String, String)], Option[String]](fields.get(name))

      def readIntOpt(name: String): V[Option[Int]] =
        readStringOpt(name).andThen{
          case Some(s) ⇒
            Validated.catchOnly[NumberFormatException](Some(s.toInt)).leftMap(e ⇒ NonEmptyList.of(name → e.getMessage))
          case None ⇒
            None.validNel
        }

      def readInt(name: String): V[Int] =
        readString(name)
          .andThen(s ⇒
            Validated.catchOnly[NumberFormatException](s.toInt).leftMap(e ⇒ NonEmptyList.of(name → e.getMessage)))

      (readString("title") |@| readStringOpt("headline") |@| readIntOpt("publishedAt")).map { (title, headline, publishedAt) ⇒
        if (article.title != title ||
          article.headline != headline ||
          article.publishedYear != publishedAt) A.update(article.id, title, headline, publishedAt)
        else Free.pure[F, Article](article)
      }.sequence
    }

  def update[F[_]](articleId: Int, fields: Map[String, String], author: Person)(implicit A: ArticleOps[F], As: AliasOps[F]): Free[F, Validated[NonEmptyList[(String, String)], (Article, Option[Alias])]] =
    for {
      article ← A.getById(articleId)
      aText ← updateText(article, fields.get("text"), author)
      aUp ← updateArticle(aText, fields, author)
      al ← fields.get("alias").map(_.trim).filter(_.nonEmpty) match {
        case Some(alias) if aUp.isValid ⇒ As.tryPointTo(alias, article, force = true)
        case _                          ⇒ As.findAlias(article)
      }
    } yield aUp.map(_ → al)

  def editArticleHtml[F[_]](articleId: Int)(implicit A: ArticleOps[F], As: AliasOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
      alias ← As.findAlias(article)
    } yield html.editArticle(article, text, alias.map(_.id), Map.empty)

  def articleHtml[F[_]](articleId: Int)(implicit A: ArticleOps[F], Au: PersonOps[F], Im: ImageOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
      author ← Au.getById(article.authorIds.head) // TODO: show all authors
      cover ← article.coverId match {
        case Some(coverId) ⇒ Im.find(coverId)
        case None          ⇒ Free.pure[F, Option[Image]](None)
      }
    } yield html.article(article, text, author, cover)

  def articlesHtml[F[_]](offset: Int, limit: Int, authorId: Option[Int], q: Option[String])(implicit A: ArticleOps[F], Au: PersonOps[F]): Free[F, Html] =
    for {
      articles ← A.fetch(authorId, q, offset, limit)
      authors ← Au.getByIds(articles.values.flatMap(_.authorIds.toList).toSet)
    } yield html.articles(articles, authors)
}
