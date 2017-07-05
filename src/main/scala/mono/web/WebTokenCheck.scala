package mono.web

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import cats.free.Free
import cats.~>
import monix.cats._
import monix.eval.Task
import monix.execution.Scheduler
import mono.core.person.{ Person, PersonOps }
import mono.core.env.EnvOps

import scala.language.higherKinds
import scala.util.Try

object WebTokenCheck {

  def checkToken[F[_]](action: String)(implicit Au: PersonOps[F], E: EnvOps[F], i: F ~> Task, s: Scheduler): Directive1[Person] =
    parameter('token).flatMap { token ⇒
      onSuccess((for {
        claim ← E.parseToken(token)
        authorOpt ← claim match {
          case Some(c) if c.issuer.contains("bot") && c.audience.exists(_.contains(action)) && c.subject.isDefined ⇒
            Au.getById(c.subject.get.toInt).map(Option(_))
          case v ⇒
            Free.pure[F, Option[Person]](None)
        }
      } yield authorOpt).foldMap(i).runAsync).flatMap {
        case Some(a) ⇒
          provide(a)
        case None ⇒
          reject
      }
    }

  def tokenUserOpt[F[_]](implicit E: EnvOps[F], i: F ~> Task, s: Scheduler): Directive1[Option[Int]] =
    optionalHeaderValueByType[Authorization]().flatMap {
      case Some(Authorization(OAuth2BearerToken(token))) ⇒
        onSuccess(
          E.parseToken(token)
          .map(_.flatMap(_.subject).flatMap(v ⇒ Try(v.toInt).toOption)).foldMap(i)
          .runAsync
        )
      case _ ⇒
        provide(Option.empty[Int])
    }

}
