package mono.core.article

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import cats.data.NonEmptyList
import cats.~>
import monix.eval.Task

import scala.collection.concurrent.TrieMap

class ArticlesInMemoryInterpreter extends (ArticleOp ~> Task) {
  private val articles = TrieMap.empty[Long, Article]
  private val texts = TrieMap.empty[Long, String]

  private def drafts(id: Long): List[Article] =
    articles.values.filter(a ⇒ a.authorIds.toList.contains(id) && a.isDraft).toList

  private def pubs: List[Article] =
    articles.values.filterNot(_.isDraft).toList

  private val id = new AtomicInteger(0)

  override def apply[A](fa: ArticleOp[A]): Task[A] = (fa match {
    case CreateArticle(user, lang, title, createdAt) ⇒
      val a = Article(id.getAndIncrement(), NonEmptyList.of(user), lang, title, None, None, None, Nil, createdAt, createdAt, None, None, 0, isDraft = true)
      articles.put(a.id, a)
      Task.now(a)

    case FetchArticles(authorId, q, offset, limit) ⇒
      val filtered = pubs.filter(a ⇒
        authorId.fold(true)(a.authorIds.toList.contains))
      Task.now(Articles(filtered.slice(offset, offset + limit), filtered.size))

    case GetArticleById(i) ⇒
      articles.get(i) match {
        case Some(a) ⇒ Task.now(a)
        case None    ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case FetchDrafts(authorId, offset, limit) ⇒
      val ds = drafts(authorId)
      Task.now(Articles(ds.slice(offset, offset + limit), ds.size))

    case PublishDraft(i, y, publishedAt) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            isDraft = false,
            publishedYear = a.publishedYear.orElse(Some(y)),
            publishedAt = a.publishedAt.orElse(Some(publishedAt))
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case DraftArticle(i) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(isDraft = true)
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case GetText(i) ⇒
      Task.now(texts.getOrElse(i, ""))

    case SetText(i, t) ⇒
      texts(i) = t
      articles(i) = articles(i).copy(modifiedAt = Instant.now(), version = articles(i).version + 1)
      Task.now(t)

    case SetCover(i, c) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            coverId = c,
            modifiedAt = Instant.now(),
            version = a.version + 1
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case UpdateArticle(i, title, headline, description, publishedYear) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            title = title,
            headline = headline,
            description = description,
            publishedYear = publishedYear,
            version = a.version + 1,
            modifiedAt = Instant.now()
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case AddImage(i, imageId) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            imageIds = (a.imageIds :+ imageId).distinct,
            version = a.version + 1,
            modifiedAt = Instant.now()
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case RemoveImage(i, imageId) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            imageIds = a.imageIds.filterNot(_ == imageId),
            version = a.version + 1,
            modifiedAt = Instant.now()
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case DeleteArticle(i) ⇒
      Task.now(articles.remove(i).isDefined)

  }).map(_.asInstanceOf[A])
}
