package mono.core.bus

import cats.free.{ Free, Inject }
import cats.free.Free.inject

import scala.language.higherKinds

class EventBusOps[F[_]](implicit I: Inject[EventBusOp, F]) {

  def emitAuth(id: String, token: String): Free[F, Unit] =
    inject[EventBusOp, F](EmitAuth(id, token))

  def waitAuth(id: String): Free[F, String] =
    inject[EventBusOp, F](WaitAuth(id))

}

object EventBusOps {
  implicit def ops[F[_]](implicit I: Inject[EventBusOp, F]): EventBusOps[F] = new EventBusOps[F]
}