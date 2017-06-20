package mono.web

import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.free.Free
import cats.~>
import monix.eval.Task
import mono.article.ArticleOps
import mono.author.AuthorOps

import scala.language.higherKinds
import monix.execution.Scheduler.Implicits.global
import play.twirl.api.Html

class WebArticle[F[_]](implicit A: ArticleOps[F], Au: AuthorOps[F]) extends Web[F] {

  import WebArticle._
  import WebTokenCheck._

  override def route(implicit i: F ~> Task): Route = path(LongNumber) { articleId ⇒
    // TODO: check permissions
    articleHtml[F](articleId)
  } ~ path("edit" / LongNumber){ articleId ⇒
    checkToken[F](s"edit/" + articleId).apply { maybeAuthorF ⇒
      // TODO: check permissions
      get {
        editArticleHtml[F](articleId)
      } ~ (post & entity(as[FormData])) { d ⇒
        updateArticleHtml[F](articleId, d.fields.toMap)
      }
    }
  } ~ (pathEndOrSingleSlash & parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'authorId.as[Long].?, 'q.?)) { (o, l, a, q) ⇒
    articlesHtml(o, l, a, q)
  }

}

object WebArticle {
  def updateArticleHtml[F[_]](articleId: Long, fields: Map[String, String])(implicit A: ArticleOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
      _ ← A.setTitle(articleId, fields.getOrElse("title", article.title))
      _ ← A.setHeadline(articleId, fields.get("headline"))
      _ ← A.setText(articleId, fields.getOrElse("text", text))
      html ← editArticleHtml(articleId)
    } yield html

  def editArticleHtml[F[_]](articleId: Long)(implicit A: ArticleOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
    } yield html.editArticle(article, text)

  def articleHtml[F[_]](articleId: Long)(implicit A: ArticleOps[F], Au: AuthorOps[F]): Free[F, Html] =
    for {
      article ← A.getById(articleId)
      text ← A.getText(articleId)
      author ← Au.getById(article.authorId)
    } yield html.article(article, text, author)

  def articlesHtml[F[_]](offset: Int, limit: Int, authorId: Option[Long], q: Option[String])(implicit A: ArticleOps[F], Au: AuthorOps[F]): Free[F, Html] =
    for {
      articles ← A.fetch(authorId, q, offset, limit)
      authors ← Au.getByIds(articles.values.map(_.authorId).toSet)
    } yield html.articles(articles, authors)
}
