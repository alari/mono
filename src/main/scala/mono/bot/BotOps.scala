package mono.bot

import cats.free.Free.inject
import cats.free.{ Free, Inject }
import scala.language.higherKinds

class BotOps[F[_]](implicit I: Inject[BotOp, F]) {

  def say(text: String, chatId: Long): Free[F, Long] =
    inject[BotOp, F](Say(text, chatId))

  def say(text: String, meta: Incoming.Meta): Free[F, Long] =
    say(text, meta.chat.id)

  def reply(text: String, meta: Incoming.Meta, forceReply: Boolean = false): Free[F, Long] =
    inject[BotOp, F](Reply(text, meta, forceReply))

}

object BotOps {
  implicit def ops[F[_]](implicit I: Inject[BotOp, F]): BotOps[F] = new BotOps[F]
}
