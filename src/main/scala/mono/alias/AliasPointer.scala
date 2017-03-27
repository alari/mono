package mono.alias

import scala.language.implicitConversions

sealed trait AliasPointer

object AliasPointer {
  case class Article(id: Long) extends AliasPointer
  case class Author(id: Long) extends AliasPointer

  implicit def fromArticle(a: mono.article.Article): AliasPointer = Article(a.id)
  implicit def fromAuthor(a: mono.author.Author): AliasPointer = Author(a.id)
}
