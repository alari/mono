package mono.bot

sealed trait BotOp[T] {
  val chatId: Long
}

case class Say(text: String, chatId: Long) extends BotOp[Long]

case class Reply(text: String, meta: Incoming.Meta, forceReply: Boolean) extends BotOp[Long] {
  override val chatId: Long = meta.chat.id
}