package mono.article

import java.time.Instant

sealed trait ArticleOp[T]

case class CreateArticle(
  authorId:    Long,
  title:       String,
  description: Option[String],
  createdAt:   Instant
) extends ArticleOp[Article]

case class FetchArticles(
  authorId: Option[Long],
  q:        Option[String],
  offset:   Int,
  limit:    Int
) extends ArticleOp[Articles]

case class GetArticleById(id: Long) extends ArticleOp[Article]