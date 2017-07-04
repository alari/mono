package mono.api

import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import mono.core.alias.{ Alias, AliasOps, AliasPointer }
import mono.core.article.{ Article, ArticleOps, Articles }
import mono.core.image.{ Image, ImageOps }
import mono.core.person.{ Person, PersonOps }

import scala.language.higherKinds

abstract class GraphQLContext {
  def getPersonById(id: Int): Task[Person]

  def getPersonsByIds(ids: Seq[Int]): Task[Seq[Person]]

  def getArticleById(id: Int): Task[Article]

  def getArticleText(article: Article): Task[String]

  def fetchArticles(authorId: Option[Int], q: Option[String], offset: Int, limit: Int): Task[Articles]

  def getImageById(id: Int): Task[Image]

  def getImagesByIds(ids: Seq[Int]): Task[Seq[Image]]

  def getAliasById(id: String): Task[Alias]

  def getAliases(pointers: Seq[AliasPointer]): Task[Seq[Alias]]
}

object GraphQLContext extends MonixToCatsConversions {
  private implicit def freeToTask[F[_], V](f: Free[F, V])(implicit i: F ~> Task): Task[V] =
    f.foldMap(i)

  def apply[F[_]]()(
    implicit
    P:  PersonOps[F],
    A:  ArticleOps[F],
    I:  ImageOps[F],
    Al: AliasOps[F],
    i:  F ~> Task
  ): GraphQLContext = new GraphQLContext {
    override def getPersonById(id: Int): Task[Person] =
      P.getById(id)

    override def getPersonsByIds(ids: Seq[Int]): Task[Seq[Person]] =
      P.getByIds(ids.toSet).map(vs ⇒ ids.map(vs))

    // TODO: check permissions
    override def getArticleById(id: Int): Task[Article] =
      A.getById(id)

    // TODO: check permissions
    override def getArticleText(article: Article): Task[String] =
      A.getText(article.id)

    override def fetchArticles(authorId: Option[Int], q: Option[String], offset: Int, limit: Int): Task[Articles] =
      A.fetch(authorId, q, offset, limit)

    override def getImageById(id: Int): Task[Image] =
      I.getById(id)

    override def getImagesByIds(ids: Seq[Int]): Task[Seq[Image]] =
      I.getByIds(ids).map(_.map(im ⇒ im.id → im).toMap).map(ims ⇒ ids.map(ims))

    override def getAliasById(id: String): Task[Alias] =
      Al.getAlias(id)

    override def getAliases(pointers: Seq[AliasPointer]): Task[Seq[Alias]] =
      Al.findAliases(pointers: _*).map(_.values.toSeq)
  }
}
