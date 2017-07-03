package mono.core.article

import java.time.{ Instant, LocalDateTime }

import cats.free.Free.inject
import cats.free.{ Free, Inject }

import scala.language.higherKinds

class ArticleOps[F[_]](implicit I: Inject[ArticleOp, F]) {

  def create(
    authorId:  Int,
    title:     String,
    createdAt: Instant
  ): Free[F, Article] =
    inject[ArticleOp, F](CreateArticle(authorId, title, createdAt))

  def fetch(
    authorId: Option[Int],
    q:        Option[String],
    offset:   Int,
    limit:    Int
  ): Free[F, Articles] =
    inject[ArticleOp, F](FetchArticles(authorId, q, offset, limit))

  def getById(id: Int): Free[F, Article] =
    inject[ArticleOp, F](GetArticleById(id))

  def fetchDrafts(
    authorId: Int,
    offset:   Int,
    limit:    Int
  ): Free[F, Articles] =
    inject[ArticleOp, F](FetchDrafts(authorId, offset, limit))

  def publishDraft(id: Int, publishedYear: Int = LocalDateTime.now().getYear): Free[F, Article] =
    inject[ArticleOp, F](PublishDraft(id, publishedYear))

  def draftArticle(id: Int): Free[F, Article] =
    inject[ArticleOp, F](DraftArticle(id))

  def getText(id: Int): Free[F, String] =
    inject[ArticleOp, F](GetText(id))

  def setText(id: Int, text: String): Free[F, String] =
    inject[ArticleOp, F](SetText(id, text))

  def setCover(id: Int, coverId: Option[Int]): Free[F, Article] =
    inject[ArticleOp, F](SetCover(id, coverId))

  def update(id: Int, title: String, headline: Option[String], publishedAt: Option[Int]): Free[F, Article] =
    inject[ArticleOp, F](UpdateArticle(id, title, headline, publishedAt))

  def addImage(id: Int, imageId: Int): Free[F, Article] =
    inject[ArticleOp, F](AddImage(id, imageId))

  def removeImage(id: Int, imageId: Int): Free[F, Article] =
    inject[ArticleOp, F](RemoveImage(id, imageId))

  def delete(id: Int): Free[F, Boolean] =
    inject[ArticleOp, F](DeleteArticle(id))
}

object ArticleOps {
  implicit def ops[F[_]](implicit I: Inject[ArticleOp, F]): ArticleOps[F] = new ArticleOps[F]
}