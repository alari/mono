package mono.web

import akka.http.scaladsl.server.{ Directives, Route, StandardRoute }
import Directives._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, ResponseEntity }
import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import mono.article.{ ArticleOp, ArticleOps }

import scala.language.higherKinds
import monix.execution.Scheduler.Implicits._
import play.twirl.api.Html

class WebArticle[F[_]](interpreter: F ~> Task)(implicit A: ArticleOps[F]) extends MonixToCatsConversions {

  private def run(program: Free[F, Html]): StandardRoute = complete(
    program
    .foldMap(interpreter)
    .map(h ⇒ HttpEntity(ContentTypes.`text/html(UTF-8)`, h.toString()))
    .runAsync
  )

  val route: Route = path(LongNumber) { articleId ⇒
    run(
      for {
        a ← A.get(articleId)
      } yield html.article(a)
    )

  } ~ parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'user.?, 'q.?) { (o, l, u, q) ⇒
    run(
      for {
        as ← A.fetch(u, q, o, l)
      } yield html.articles(as)
    )
  }

}
