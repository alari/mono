package mono.bot

import info.mukel.telegrambot4s.models.Message

abstract sealed class Incoming {
  val meta: Incoming.Meta
}

case class Plain(text: String, meta: Incoming.Meta) extends Incoming

case class Command(name: String, tail: Option[String], meta: Incoming.Meta) extends Incoming

case class Unknown(meta: Incoming.Meta) extends Incoming

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

  private val commandR = "/([a-zA-Z]+)(@[^ ]+)?( .+)?".r

  def telegram(msg: Message): Incoming = {
    val c = Chat(msg.chat.id, msg.chat.username, msg.chat.title.orElse(msg.chat.firstName))
    val m = Meta(msg.messageId, c)

    msg.text match {
      case Some(text) ⇒
        console(text, m)

      case _ ⇒
        Unknown(m)
    }
  }

  def console(
    in:   String,
    meta: Incoming.Meta = Meta(0l, Chat(0l, Some(System.getProperty("user.name")), Some(System.getProperty("user.name"))))
  ): Incoming =
    in.trim match {
      case commandR(name, _, rest) ⇒
        Command(name, Option(rest).map(_.trim).filter(_.nonEmpty), meta)

      case text ⇒
        Plain(text, meta)
    }
}