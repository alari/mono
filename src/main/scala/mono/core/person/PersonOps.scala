package mono.core.person

import cats.free.{ Free, Inject }
import cats.free.Free.inject

import scala.language.higherKinds

class PersonOps[F[_]](implicit Au: Inject[PersonOp, F]) {

  def ensureTelegram(
    telegramId: Long, name: String
  ): Free[F, Person] =
    inject[PersonOp, F](EnsureTelegramPerson(telegramId, name))

  def getById(id: Int): Free[F, Person] =
    inject[PersonOp, F](GetPersonById(id))

  def getByIds(ids: Set[Int]): Free[F, Map[Int, Person]] =
    inject[PersonOp, F](GetPersonsByIds(ids))

  def findByTelegramId(telegramId: Long): Free[F, Option[Person]] =
    inject[PersonOp, F](FindPersonByTelegramId(telegramId))
}

object PersonOps {
  implicit def ops[F[_]](implicit P: Inject[PersonOp, F]): PersonOps[F] = new PersonOps[F]
}