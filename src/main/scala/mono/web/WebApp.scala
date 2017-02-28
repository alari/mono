package mono.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import cats.~>
import monix.eval.Task
import mono.article.ArticleOps
import mono.author.AuthorOps

import scala.language.higherKinds

class WebApp[F[_]](interpreter: F ~> Task)(implicit A: ArticleOps[F], Au: AuthorOps[F]) {

  val route: Route =
    new WebArticle[F](interpreter).route

  def run()(implicit system: ActorSystem, mat: Materializer) =
    Http().bindAndHandle(route, "localhost", 9000)

}
