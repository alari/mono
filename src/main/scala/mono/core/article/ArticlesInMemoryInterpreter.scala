package mono.core.article

import java.time.{ Instant, LocalDateTime }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }

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
    case CreateArticle(user, title, createdAt) ⇒
      val a = Article(id.getAndIncrement(), NonEmptyList.of(user), title, None, None, Nil, createdAt, createdAt, None, 0, isDraft = true)
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

    case PublishDraft(i, y) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            isDraft = false,
            publishedYear = a.publishedYear.orElse(Some(y))
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

    case SetTitle(i, t) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            title = t,
            modifiedAt = Instant.now(),
            version = a.version + 1
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

    case SetHeadline(i, t) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            headline = t,
            modifiedAt = Instant.now(),
            version = a.version + 1
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

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

    case UpdateArticle(i, title, headline, publishedYear) ⇒
      articles.get(i) match {
        case Some(a) ⇒
          val aa = a.copy(
            title = title,
            headline = headline,
            publishedYear = publishedYear,
            version = a.version + 1,
            modifiedAt = Instant.now()
          )
          articles(i) = aa
          Task.now(aa)
        case None ⇒ Task.raiseError(new NoSuchElementException("ID not found: " + i))
      }

  }).map(_.asInstanceOf[A])
}
