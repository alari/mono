package mono.web

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import cats.free.Free
import mono.author.{ Author, AuthorOps }
import scala.language.higherKinds

object WebTokenCheck {

  def checkToken[F[_]](action: String)(implicit Au: AuthorOps[F]): Directive1[Free[F, Option[Author]]] =
    parameter('token).map(_.split(':').toList).flatMap {
      case issuer :: subject :: audience :: Nil if issuer == "telegram" && audience.equalsIgnoreCase(action) ⇒
        provide(Au.findByTelegramId(subject.toLong))

      case _ ⇒
        reject
    }

}
