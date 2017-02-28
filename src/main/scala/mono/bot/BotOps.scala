package mono.bot

import cats.free.Free.inject
import cats.free.{ Free, Inject }
import scala.language.higherKinds

class BotOps[F[_]](implicit I: Inject[BotOp, F]) {

  def say(text: String, chatId: Long): Free[F, Unit] =
    inject[BotOp, F](Say(text, chatId))

}

object BotOps {
  implicit def ops[F[_]](implicit I: Inject[BotOp, F]): BotOps[F] = new BotOps[F]
}
