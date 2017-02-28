package mono.bot

import info.mukel.telegrambot4s.models.Message

abstract sealed class Incoming {
  val chatId: Long
}

case class Plain(text: String, chatId: Long) extends Incoming

object Incoming {
  def telegram(msg: Message): Incoming = msg.text.map(Plain(_, msg.chat.id)).getOrElse(new Incoming {
    override val chatId = msg.chat.id
  })
}