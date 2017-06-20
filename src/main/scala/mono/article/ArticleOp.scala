package mono.article

import java.time.Instant

sealed trait ArticleOp[T]

case class CreateArticle(
  authorId:  Long,
  title:     String,
  createdAt: Instant
) extends ArticleOp[Article]

case class FetchArticles(
  authorId: Option[Long],
  q:        Option[String],
  offset:   Int,
  limit:    Int
) extends ArticleOp[Articles]

case class FetchDrafts(
  authorId: Long,
  offset:   Int,
  limit:    Int
) extends ArticleOp[Articles]

case class PublishDraft(id: Long) extends ArticleOp[Article]

case class DraftArticle(id: Long) extends ArticleOp[Article]

case class GetArticleById(id: Long) extends ArticleOp[Article]

case class GetText(id: Long) extends ArticleOp[String]

case class SetText(id: Long, text: String) extends ArticleOp[String]

case class SetTitle(id: Long, text: String) extends ArticleOp[Article]

case class SetHeadline(id: Long, text: Option[String]) extends ArticleOp[Article]