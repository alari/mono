package mono.alias

sealed trait AliasPointer

object AliasPointer {
  case class Article(id: Long) extends AliasPointer
  case class Author(id: Long) extends AliasPointer
}
