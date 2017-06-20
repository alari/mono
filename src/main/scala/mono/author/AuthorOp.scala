package mono.author

sealed trait AuthorOp[T]

case class EnsureTelegramAuthor(telegramId: Long, title: String) extends AuthorOp[Author]

case class FindAuthorByTelegramId(telegramId: Long) extends AuthorOp[Option[Author]]

case class GetAuthorById(id: Long) extends AuthorOp[Author]

case class GetAuthorsByIds(ids: Set[Long]) extends AuthorOp[Map[Long, Author]]