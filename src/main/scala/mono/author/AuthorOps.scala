package mono.author

import cats.free.{ Free, Inject }
import cats.free.Free.inject

import scala.language.higherKinds

class AuthorOps[F[_]](implicit Au: Inject[AuthorOp, F]) {

  def ensureTelegram(
    telegramId: Long, title: String
  ): Free[F, Author] =
    inject[AuthorOp, F](EnsureTelegramAuthor(telegramId, title))

  def getById(id: Long): Free[F, Author] =
    inject[AuthorOp, F](GetAuthorById(id))

  def getByIds(ids: Set[Long]): Free[F, Map[Long, Author]] =
    inject[AuthorOp, F](GetAuthorsByIds(ids))
}

object AuthorOps {
  implicit def ops[F[_]](implicit Au: Inject[AuthorOp, F]): AuthorOps[F] = new AuthorOps[F]
}