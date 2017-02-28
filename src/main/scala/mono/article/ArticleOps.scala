package mono.article

import java.time.Instant

import cats.free.Free.inject
import cats.free.{ Free, Inject }
import scala.language.higherKinds

class ArticleOps[F[_]](implicit I: Inject[ArticleOp, F]) {

  def create(
    user:        String,
    title:       String,
    description: Option[String],
    createdAt:   Instant
  ): Free[F, Article] =
    inject[ArticleOp, F](CreateArticle(user, title, description, createdAt))

  def fetch(
    user:   Option[String],
    q:      Option[String],
    offset: Int,
    limit:  Int
  ): Free[F, Articles] =
    inject[ArticleOp, F](FetchArticles(user, q, offset, limit))

  def get(id: Long): Free[F, Article] =
    inject[ArticleOp, F](GetArticle(id))
}

object ArticleOps {
  implicit def ops[F[_]](implicit I: Inject[ArticleOp, F]): ArticleOps[F] = new ArticleOps[F]
}