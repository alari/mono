package mono.alias

import scala.language.implicitConversions

sealed trait AliasPointer

object AliasPointer {
  case class Article(id: Int) extends AliasPointer
  case class Person(id: Int) extends AliasPointer

  implicit def fromArticle(a: mono.article.Article): AliasPointer = Article(a.id)
  implicit def fromPerson(a: mono.person.Person): AliasPointer = Person(a.id)
}
