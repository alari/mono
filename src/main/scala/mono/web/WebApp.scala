package mono.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import cats.~>
import monix.eval.Task
import mono.core.article.ArticleOps
import mono.core.person.PersonOps
import akka.http.scaladsl.server.Directives._
import mono.core.alias.AliasOps
import mono.core.env.EnvOps
import mono.core.image.ImageOps

import scala.language.higherKinds

class WebApp[F[_]](implicit A: ArticleOps[F], Au: PersonOps[F], As: AliasOps[F], Im: ImageOps[F], E: EnvOps[F]) extends Web[F] {

  override def route(implicit i: F ~> Task): Route =
    new WebArticle[F].route ~
      new WebImage[F].route ~
      new WebAlias[F].route ~
      getFromResourceDirectory("web")

  def run(host: String = "localhost", port: Int = 9000)(implicit i: F ~> Task, system: ActorSystem, mat: Materializer) =
    Http().bindAndHandle(route, "localhost", 9000)

}
