package mono.api

import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import monix.reactive.Observable
import mono.core.alias.{ Alias, AliasOps, AliasPointer }
import mono.core.article.{ Article, ArticleOps, Articles }
import mono.core.bus.EventBusOps
import mono.core.image.{ Image, ImageOps }
import mono.core.person.{ Person, PersonOps }

import scala.language.higherKinds
import scala.concurrent.duration._

abstract class GraphQLContext {
  private[api] def currentUserId: Option[Int]

  def getPersonById(id: Int): Task[Person]

  def getPersonsByIds(ids: Seq[Int]): Task[Seq[Person]]

  def getArticleById(id: Int): Task[Article]

  def getArticleText(article: Article): Task[String]

  def fetchArticles(authorId: Option[Int], q: Option[String], offset: Int, limit: Int): Task[Articles]

  def fetchDrafts(offset: Int, limit: Int): Task[Articles]

  def getImageById(id: Int): Task[Image]

  def getImagesByIds(ids: Seq[Int]): Task[Seq[Image]]

  def getAliasById(id: String): Task[Alias]

  def getAliases(pointers: Seq[AliasPointer]): Task[Seq[Alias]]

  def trackTelegramAuth(token: String): Observable[String]
}

object GraphQLContext extends MonixToCatsConversions {
  private implicit def freeToTask[F[_], V](f: Free[F, V])(implicit i: F ~> Task): Task[V] =
    f.foldMap(i)

  def apply[F[_]](_currentUserId: Option[Int])(
    implicit
    P:  PersonOps[F],
    A:  ArticleOps[F],
    I:  ImageOps[F],
    Al: AliasOps[F],
    Eb: EventBusOps[F],
    i:  F ~> Task
  ): GraphQLContext = new GraphQLContext {
    override private[api] def currentUserId = _currentUserId

    override def getPersonById(id: Int): Task[Person] =
      P.getById(id)

    override def getPersonsByIds(ids: Seq[Int]): Task[Seq[Person]] =
      P.getByIds(ids.toSet).map(vs ⇒ ids.map(vs))

    // TODO: use permissions control lib
    override def getArticleById(id: Int): Task[Article] =
      A.getById(id).map { a ⇒
        if (!a.isDraft || currentUserId.exists(a.authorIds.toList.contains)) a
        else throw new IllegalAccessError("Inaccessible")
      }

    // TODO: use permissions control lib
    override def getArticleText(article: Article): Task[String] =
      if (!article.isDraft || currentUserId.exists(article.authorIds.toList.contains)) A.getText(article.id)
      else throw new IllegalAccessError("Inaccessible")

    override def fetchArticles(authorId: Option[Int], q: Option[String], offset: Int, limit: Int): Task[Articles] =
      A.fetch(authorId, q, offset, limit)

    override def fetchDrafts(offset: Int, limit: Int): Task[Articles] =
      currentUserId match {
        case Some(uid) ⇒ A.fetchDrafts(uid, offset, limit)
        case None      ⇒ Task.raiseError(new IllegalAccessError("Inaccessible"))
      }

    override def getImageById(id: Int): Task[Image] =
      I.getById(id)

    override def getImagesByIds(ids: Seq[Int]): Task[Seq[Image]] =
      I.getByIds(ids).map(_.map(im ⇒ im.id → im).toMap).map(ims ⇒ ids.map(ims))

    override def getAliasById(id: String): Task[Alias] =
      Al.getAlias(id)

    override def getAliases(pointers: Seq[AliasPointer]): Task[Seq[Alias]] =
      Al.findAliases(pointers: _*).map(_.values.toSeq)

    override def trackTelegramAuth(token: String): Observable[String] =
      Observable.fromTask(Eb.waitAuth(token))
  }
}
