package mono.article

import java.time.Instant

import cats.free.Free.inject
import cats.free.{ Free, Inject }

import scala.language.higherKinds

class ArticleOps[F[_]](implicit I: Inject[ArticleOp, F]) {

  def create(
    authorId:  Long,
    title:     String,
    createdAt: Instant
  ): Free[F, Article] =
    inject[ArticleOp, F](CreateArticle(authorId, title, createdAt))

  def fetch(
    authorId: Option[Long],
    q:        Option[String],
    offset:   Int,
    limit:    Int
  ): Free[F, Articles] =
    inject[ArticleOp, F](FetchArticles(authorId, q, offset, limit))

  def getById(id: Long): Free[F, Article] =
    inject[ArticleOp, F](GetArticleById(id))

  def fetchDrafts(
    authorId: Long,
    offset:   Int,
    limit:    Int
  ): Free[F, Articles] =
    inject[ArticleOp, F](FetchDrafts(authorId, offset, limit))

  def publishDraft(id: Long): Free[F, Article] =
    inject[ArticleOp, F](PublishDraft(id))

  def draftArticle(id: Long): Free[F, Article] =
    inject[ArticleOp, F](DraftArticle(id))

  def getText(id: Long): Free[F, String] =
    inject[ArticleOp, F](GetText(id))

  def setText(id: Long, text: String): Free[F, String] =
    inject[ArticleOp, F](SetText(id, text))

  def setTitle(id: Long, text: String): Free[F, Article] =
    inject[ArticleOp, F](SetTitle(id, text))

  def setHeadline(id: Long, text: Option[String]): Free[F, Article] =
    inject[ArticleOp, F](SetHeadline(id, text))
}

object ArticleOps {
  implicit def ops[F[_]](implicit I: Inject[ArticleOp, F]): ArticleOps[F] = new ArticleOps[F]
}