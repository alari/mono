package mono.person

import java.time.Instant

import cats.~>
import monix.eval.Task
import doobie.imports._
import cats.data._, cats.implicits._
import fs2.interop.cats._

object PersonsDoobieInterpreter {
  val createTable: Update0 =
    sql"CREATE TABLE IF NOT EXISTS persons(id SERIAL PRIMARY KEY, telegram_id BIGINT, name TEXT NOT NULL, created_at TIMESTAMP)".update

  val createTelegramIdIndex: Update0 =
    sql"CREATE UNIQUE INDEX IF NOT EXISTS persons_telegram_id_uindex ON persons (telegram_id)".update

  def init(xa: Transactor[Task]): Task[Int] =
    (createTable.run *> createTelegramIdIndex.run).transact(xa)

  def getPersonByIdQuery(id: Int): Query0[Person] =
    sql"SELECT id, telegram_id, name, created_at FROM persons WHERE id = $id"
      .query[Person]

  def getPersonsByIdsQuery(ids: NonEmptyList[Int]): Query0[Person] =
    (sql"SELECT id, telegram_id, name, created_at FROM persons WHERE " ++ Fragments.in(fr"id", ids))
      .query[Person]

  def getPersonByTelegramIdQuery(telegramId: Long): Query0[Person] =
    sql"SELECT id, telegram_id, name, created_at FROM persons WHERE telegram_id = $telegramId"
      .query[Person]

  def insertPersonQuery(telegramId: Long, name: String, createdAt: Instant): ConnectionIO[Person] =
    sql"INSERT INTO persons(telegram_id, name, created_at) VALUES ($telegramId, $name, $createdAt)"
      .update.withUniqueGeneratedKeys("id", "telegram_id", "name", "created_at")
}

class PersonsDoobieInterpreter(xa: Transactor[Task]) extends (PersonOp ~> Task) {

  import PersonsDoobieInterpreter._

  override def apply[A](fa: PersonOp[A]): Task[A] = fa match {
    case EnsureTelegramPerson(telegramId, name) ⇒
      (for {
        maybePerson ← getPersonByTelegramIdQuery(telegramId).option
        person ← maybePerson match {
          case Some(a) ⇒ a.pure[ConnectionIO]
          case None    ⇒ insertPersonQuery(telegramId, name, Instant.now())
        }
      } yield person.asInstanceOf[A]).transact(xa)

    case GetPersonById(id) ⇒
      getPersonByIdQuery(id).option.transact(xa).flatMap {
        case Some(a) ⇒ Task.now(a)
        case None    ⇒ Task.raiseError(new NoSuchElementException(s"Id not found: $id"))
      }.asInstanceOf[Task[A]]

    case GetPersonsByIds(ids) ⇒
      NonEmptyList.fromList(ids.toList)
        .fold(Task.now(Map.empty[Int, Person]))(idsNel ⇒
          getPersonsByIdsQuery(idsNel).to[Seq].transact(xa)
            .map(_.map(a ⇒ (a.id, a)).toMap)).asInstanceOf[Task[A]]

    case FindPersonByTelegramId(telegramId) ⇒
      getPersonByTelegramIdQuery(telegramId)
        .option.transact(xa).asInstanceOf[Task[A]]
  }
}
