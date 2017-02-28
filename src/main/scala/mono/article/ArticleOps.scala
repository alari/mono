package mono.article

import java.time.Instant

import cats.free.Free.inject
import cats.free.{ Free, Inject }
import scala.language.higherKinds

class ArticleOps[F[_]](implicit I: Inject[ArticleOp, F]) {

  def create(
    authorId:    Long,
    title:       String,
    description: Option[String],
    createdAt:   Instant
  ): Free[F, Article] =
    inject[ArticleOp, F](CreateArticle(authorId, title, description, createdAt))

  def fetch(
    authorId: Option[Long],
    q:        Option[String],
    offset:   Int,
    limit:    Int
  ): Free[F, Articles] =
    inject[ArticleOp, F](FetchArticles(authorId, q, offset, limit))

  def getById(id: Long): Free[F, Article] =
    inject[ArticleOp, F](GetArticleById(id))
}

object ArticleOps {
  implicit def ops[F[_]](implicit I: Inject[ArticleOp, F]): ArticleOps[F] = new ArticleOps[F]
}