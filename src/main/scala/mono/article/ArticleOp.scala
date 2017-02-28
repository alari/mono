package mono.article

import java.time.Instant

sealed trait ArticleOp[T]

case class CreateArticle(
  user:        String,
  title:       String,
  description: Option[String],
  createdAt:   Instant
) extends ArticleOp[Article]

case class FetchArticles(
  user:   Option[String],
  q:      Option[String],
  offset: Int,
  limit:  Int
) extends ArticleOp[Articles]

case class GetArticle(id: Long) extends ArticleOp[Article]