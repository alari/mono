package mono.core.env

import cats.free.Free.inject
import cats.free.{ Free, Inject }
import pdi.jwt.JwtClaim

import scala.language.higherKinds

class EnvOps[F[_]](implicit I: Inject[EnvOp, F]) {

  def readHost(): Free[F, String] =
    inject[EnvOp, F](ReadEnvHost)

  def issueToken(claim: JwtClaim): Free[F, String] =
    inject[EnvOp, F](IssueToken(claim))

  def parseToken(token: String): Free[F, Option[JwtClaim]] =
    inject[EnvOp, F](ParseToken(token))
}

object EnvOps {
  implicit def ops[F[_]](implicit I: Inject[EnvOp, F]): EnvOps[F] = new EnvOps[F]

}