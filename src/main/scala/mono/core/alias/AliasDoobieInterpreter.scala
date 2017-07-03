package mono.core.alias

import java.time.Instant

import cats.~>
import doobie.imports.Transactor
import doobie.util.update.Update0
import monix.eval.Task
import doobie.imports._
import cats.data._
import cats.instances._
import cats.implicits._
import fs2.interop.cats._
import doobie.postgres.imports._

import scala.collection.concurrent.TrieMap
import scala.language.implicitConversions

object AliasDoobieInterpreter {
  private val createTable: Update0 =
    sql"CREATE TABLE IF NOT EXISTS aliases(id VARCHAR(64) PRIMARY KEY, person_id INT REFERENCES persons UNIQUE, article_id INT REFERENCES articles)".update

  def init(xa: Transactor[Task]): Task[Int] =
    createTable.run.transact(xa)

  type Tup = (String, Option[Int], Option[Int])

  implicit def tupleToAlias(tup: Tup): Alias =
    Alias(
      id = tup._1,
      pointer = (tup._2.map(AliasPointer.Person) orElse tup._3.map(AliasPointer.Article)).getOrElse(throw new IllegalArgumentException("Wrong tuple:" + tup))
    )

  def getById(id: String): Query0[Tup] =
    sql"SELECT id, person_id, article_id FROM aliases WHERE id=$id".query[Tup]

  def getByPointers(personIds: List[Int], articleIds: List[Int]): Query0[Tup] =
    (sql"SELECT id, person_id, article_id FROM aliases WHERE " ++ Fragments.or(Seq(
      NonEmptyList.fromList(personIds).map(pids ⇒ Fragments.in(fr"person_id", pids)),
      NonEmptyList.fromList(articleIds).map(aids ⇒ Fragments.in(fr"article_id", aids))
    ).flatten: _*)).query[Tup]

  def getByPointer(pointer: AliasPointer): Query0[Tup] =
    (sql"SELECT id, person_id, article_id FROM aliases WHERE " ++ pointerWhereFr(pointer)).query[Tup]

  private def pointerWhereFr(pointer: AliasPointer): Fragment = pointer match {
    case AliasPointer.Person(id)  ⇒ fr" person_id=$id"
    case AliasPointer.Article(id) ⇒ fr" article_id=$id"
  }

  def insertAlias(id: String, pointer: AliasPointer): Update0 =
    {
      val articleId: Option[Int] = pointer match {
        case AliasPointer.Article(aid) ⇒ Some(aid)
        case _                         ⇒ None
      }
      val personId: Option[Int] = pointer match {
        case AliasPointer.Person(pid) ⇒ Some(pid)
        case _                        ⇒ None
      }
      sql"INSERT INTO aliases(id, person_id, article_id) VALUES($id, $personId, $articleId)".update
    }

  def deleteAlias(id: String, pointer: AliasPointer): Update0 =
    (sql"DELETE FROM aliases WHERE id=$id AND " ++ pointerWhereFr(pointer)).update

}

class AliasDoobieInterpreter(xa: Transactor[Task]) extends (AliasOp ~> Task) {

  import AliasDoobieInterpreter._

  private val data = TrieMap.empty[String, Alias]

  override def apply[A](fa: AliasOp[A]): Task[A] = fa match {
    case GetAlias(id) ⇒
      getById(id).unique.map(t ⇒ t: Alias).transact(xa).asInstanceOf[Task[A]]

    case FindAliases(pointers) ⇒
      getByPointers(
        pointers.collect {
        case AliasPointer.Person(id) ⇒ id
      }.toList,

        pointers.collect {
        case AliasPointer.Article(id) ⇒ id
      }.toList

      ).list.map(_.map(t ⇒ t: Alias).map(t ⇒ t.pointer → t)).transact(xa).asInstanceOf[Task[A]]

    case TryPointTo(id, pointer, force) ⇒

      getByPointer(pointer).option.transact(xa).map(_.map(t ⇒ t: Alias)).flatMap {
        case Some(alias) if !force ⇒
          Task.now(Some(alias).asInstanceOf[A])

        case oldAlias ⇒
          Alias.normalize(Some(id)).flatMap {
            case None ⇒
              Task.now(oldAlias.asInstanceOf[A])

            case Some(alias) if oldAlias.exists(_.id == alias) ⇒
              Task.now(oldAlias.asInstanceOf[A])

            case Some(alias) if oldAlias.isEmpty ⇒
              (for {
                _ ← insertAlias(alias, pointer).run
                newAlias ← getById(alias).unique
              } yield Some(newAlias: Alias).asInstanceOf[A]).transact(xa)

            case Some(alias) ⇒
              (for {
                _ ← deleteAlias(oldAlias.get.id, pointer).run
                _ ← insertAlias(alias, pointer).run
                newAlias ← getById(alias).unique
              } yield Some(newAlias: Alias).asInstanceOf[A]).transact(xa)

          }
      }
  }

}
