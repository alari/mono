package mono.bot

import java.nio.file.Path

sealed trait BotOp[T] {
  val chatId: Long
}

case class Say(text: String, chatId: Long) extends BotOp[Long]

case class Reply(text: String, meta: Incoming.Meta, forceReply: Boolean) extends BotOp[Long] {
  override val chatId: Long = meta.chat.id
}

case class Choose(text: String, variants: List[List[String]], chatId: Long) extends BotOp[Long]

case class LoadFile(fileId: String) extends BotOp[Path] {
  override val chatId: Long = 0l
}