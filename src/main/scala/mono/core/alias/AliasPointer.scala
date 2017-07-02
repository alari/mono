package mono.core.alias

import scala.language.implicitConversions

sealed trait AliasPointer

object AliasPointer {
  case class Article(id: Int) extends AliasPointer
  case class Person(id: Int) extends AliasPointer

  implicit def fromArticle(a: mono.core.article.Article): AliasPointer = Article(a.id)
  implicit def fromPerson(a: mono.core.person.Person): AliasPointer = Person(a.id)
}
