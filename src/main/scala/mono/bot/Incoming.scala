package mono.bot

import info.mukel.telegrambot4s.models.Message

abstract sealed class Incoming {
  val meta: Incoming.Meta
}

case class Plain(text: String, meta: Incoming.Meta) extends Incoming

case class Command(name: String, tail: Option[String], meta: Incoming.Meta) extends Incoming

case class File(id: String, mimeType: Option[String], name: Option[String], size: Option[Int], meta: Incoming.Meta) extends Incoming

case class Image(fileId: String, caption: Option[String], meta: Incoming.Meta) extends Incoming

case class Unknown(meta: Incoming.Meta) extends Incoming

object Incoming {

  case class Chat(
    id:    Long,
    alias: Option[String],
    title: Option[String]
  )

  case class Meta(
    messageId: Long,
    chat:      Chat,
    isUpdate:  Boolean
  )

  private val commandR = "/([a-zA-Z0-9]+)(@[^ ]+)?( .+)?".r

  def telegram(msg: Message, isUpdate: Boolean = false): Incoming = {
    val c = Chat(msg.chat.id, msg.chat.username, msg.chat.title.orElse(msg.chat.firstName))
    val m = Meta(msg.messageId, c, isUpdate)

    msg.text match {
      case Some(text) ⇒
        console(text, m)

      case _ ⇒
        msg.document match {
          case Some(doc) ⇒
            File(doc.fileId, doc.mimeType, doc.fileName, doc.fileSize, m)

          case None ⇒
            msg.photo match {
              case Some(photoSizes) ⇒
                val s = photoSizes.maxBy(s ⇒ s.height * s.width)
                Image(s.fileId, msg.caption, m)

              case None ⇒ Unknown(m)
            }
        }
    }
  }

  def console(
    in: String,
    meta: Incoming.Meta = Meta(
      0l,
      Chat(0l, Some(System.getProperty("user.name")), Some(System.getProperty("user.name"))),
      isUpdate = false
    )
  ): Incoming =
    in.trim match {
      case commandR(name, _, rest) ⇒
        Command(name, Option(rest).map(_.trim).filter(_.nonEmpty), meta)

      case text ⇒
        Plain(text, meta)
    }
}