package mono

case class Page()

sealed trait Command[A]

case class GetPage(alias: String) extends Command[Page]

case class ListPageHeads(
  q:      Option[String],
  userId: Option[Long]
) extends Command[List[Page]]

case class CreateDraft()

case class PublishDraft()

case class HidePage()

case class UpdatePageHead()

case class UpdatePageBody()