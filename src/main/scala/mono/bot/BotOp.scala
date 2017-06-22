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

case class Inline(text: String, buttons: Seq[Seq[Inline.Button]], chatId: Long) extends BotOp[Long]

object Inline {
  sealed trait Button {
    def text: String
  }
  case class UrlButton(text: String, url: String) extends Button
  case class CallbackButton(text: String, callback: String) extends Button
}

case class LoadFile(fileId: String) extends BotOp[Path] {
  override val chatId: Long = 0l
}