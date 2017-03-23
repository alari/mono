package mono.bot

import java.nio.file.Path

import cats.free.Free.inject
import cats.free.{ Free, Inject }

import scala.language.higherKinds

class BotOps[F[_]](implicit I: Inject[BotOp, F]) {

  def say(text: String, chatId: Long): Free[F, Long] =
    inject[BotOp, F](Say(text, chatId))

  def say(text: String, meta: Incoming.Meta): Free[F, Long] =
    say(text, meta.chat.id)

  def choose(text: String, variants: List[List[String]], chatId: Long): Free[F, Long] =
    inject[BotOp, F](Choose(text, variants, chatId))

  def reply(text: String, meta: Incoming.Meta, forceReply: Boolean = false): Free[F, Long] =
    inject[BotOp, F](Reply(text, meta, forceReply))

  def loadFile(fileId: String): Free[F, Path] =
    inject[BotOp, F](LoadFile(fileId))

}

object BotOps {
  implicit def ops[F[_]](implicit I: Inject[BotOp, F]): BotOps[F] = new BotOps[F]
}
