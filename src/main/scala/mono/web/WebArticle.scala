package mono.web

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directives, Route, StandardRoute }
import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import monix.execution.Scheduler.Implicits._
import mono.article.ArticleOps
import mono.author.AuthorOps
import play.twirl.api.Html

import scala.language.higherKinds

class WebArticle[F[_]](interpreter: F ~> Task)(implicit A: ArticleOps[F], Au: AuthorOps[F]) extends MonixToCatsConversions {

  private def run(program: Free[F, Html]): StandardRoute = complete(
    program
    .foldMap(interpreter)
    .map(h ⇒ HttpEntity(ContentTypes.`text/html(UTF-8)`, h.toString()))
    .runAsync
  )

  val route: Route = path(LongNumber) { articleId ⇒
    run(
      for {
        article ← A.getById(articleId)
        author ← Au.getById(article.authorId)
      } yield html.article(article, author)
    )

  } ~ parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'authorId.as[Long].?, 'q.?) { (o, l, a, q) ⇒
    run(
      for {
        articles ← A.fetch(a, q, o, l)
        authors ← Au.getByIds(articles.values.map(_.authorId).toSet)
      } yield html.articles(articles, authors)
    )
  }

}
