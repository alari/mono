package mono.bot

import info.mukel.telegrambot4s.models.Message

abstract sealed class Incoming {
  val meta: Incoming.Meta
}

case class Plain(text: String, meta: Incoming.Meta) extends Incoming

object Incoming {

  case class Chat(
    id:    Long,
    alias: Option[String],
    title: Option[String]
  )

  case class Meta(
    messageId: Long,
    chat:      Chat
  )

  def telegram(msg: Message): Incoming = {
    val c = Chat(msg.chat.id, msg.chat.username, msg.chat.title.orElse(msg.chat.firstName))
    val m = Meta(msg.messageId, c)

    msg.text.map(Plain(_, m))
      .getOrElse(new Incoming {
        override val meta = m
      })
  }
}