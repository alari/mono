package mono.web

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

  override def route(implicit i: F ~> Task): Route = path(LongNumber) { articleId ⇒
    articleHtml[F](articleId)

  } ~ (pathEndOrSingleSlash & parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'authorId.as[Long].?, 'q.?)) { (o, l, a, q) ⇒
    articlesHtml(o, l, a, q)
  }

}

object WebArticle {
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
