package mono.person

import java.time.Instant

import cats.~>
import monix.eval.Task

import scala.collection.concurrent.TrieMap

class PersonsInMemoryInterpreter extends (PersonOp ~> Task) {
  private val stateById = TrieMap.empty[Long, Person]

  override def apply[A](fa: PersonOp[A]): Task[A] = fa match {
    case EnsureTelegramPerson(telegramId, title) ⇒
      Task.now(stateById.getOrElseUpdate(telegramId, Person(
        telegramId,
        telegramId,
        title,
        Instant.now()
      )).asInstanceOf[A])

    case GetPersonById(id) ⇒
      Task.now(stateById(id).asInstanceOf[A])

    case GetPersonsByIds(ids) ⇒
      Task.now(ids.toList.flatMap(stateById.get).map(a ⇒ a.id → a).toMap.asInstanceOf[A])

    case FindPersonByTelegramId(telegramId) ⇒
      Task.now(stateById.values.find(_.telegramId == telegramId).asInstanceOf[A])
  }
}
