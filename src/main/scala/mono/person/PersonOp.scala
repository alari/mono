package mono.person

sealed trait PersonOp[T]

case class EnsureTelegramPerson(telegramId: Long, name: String) extends PersonOp[Person]

case class FindPersonByTelegramId(telegramId: Long) extends PersonOp[Option[Person]]

case class GetPersonById(id: Int) extends PersonOp[Person]

case class GetPersonsByIds(ids: Set[Int]) extends PersonOp[Map[Int, Person]]