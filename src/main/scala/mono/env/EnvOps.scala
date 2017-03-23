package mono.env

import cats.free.Free.inject
import cats.free.{ Free, Inject }

import scala.language.higherKinds

class EnvOps[F[_]](implicit I: Inject[EnvOp, F]) {

  def readHost(): Free[F, String] =
    inject[EnvOp, F](ReadEnvHost)

}

object EnvOps {
  implicit def ops[F[_]](implicit I: Inject[EnvOp, F]): EnvOps[F] = new EnvOps[F]

}