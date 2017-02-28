package mono.bot

sealed trait BotOp[T] {
  val chatId: Long
}

case class Say(text: String, chatId: Long) extends BotOp[Unit]