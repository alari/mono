package mono.web

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{ Route, StandardRoute }
import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import play.twirl.api.Html

import scala.language.higherKinds
import scala.language.implicitConversions
import monix.execution.Scheduler

trait Web[F[_]] extends MonixToCatsConversions {
  protected implicit def run(program: Free[F, Html])(implicit i: F ~> Task, s: Scheduler): StandardRoute = complete(
    program
    .foldMap(i)
    .map(h ⇒ HttpEntity(ContentTypes.`text/html(UTF-8)`, h.toString()))
    .runAsync
  )

  def route(implicit i: F ~> Task): Route
}
