package mono.author

import java.time.Instant

import cats.~>
import monix.eval.Task

import scala.collection.concurrent.TrieMap

class AuthorsInMemoryInterpreter extends (AuthorOp ~> Task) {
  private val stateById = TrieMap.empty[Long, Author]

  override def apply[A](fa: AuthorOp[A]): Task[A] = fa match {
    case EnsureTelegramAuthor(telegramId, title) ⇒
      Task.now(stateById.getOrElseUpdate(telegramId, Author(
        telegramId,
        telegramId,
        title,
        Instant.now()
      )).asInstanceOf[A])

    case GetAuthorById(id) ⇒
      Task.now(stateById(id).asInstanceOf[A])

    case GetAuthorsByIds(ids) ⇒
      Task.now(ids.toList.flatMap(stateById.get).map(a ⇒ a.id → a).toMap.asInstanceOf[A])
  }
}
