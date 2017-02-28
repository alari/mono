package mono.article

import cats.~>
import monix.eval.Task

import scala.collection.mutable

object ArticlesInMemoryInterpreter extends (ArticleOp ~> Task) {
  private val articles = mutable.TreeMap[Long, Article]()
  private var id: Long = 0

  override def apply[A](fa: ArticleOp[A]): Task[A] = fa match {
    case CreateArticle(user, title, description, createdAt) ⇒
      id = id + 1
      val a = Article(id, user, title, description, createdAt)
      articles.put(a.id, a)
      Task.now(a.asInstanceOf[A])

    case FetchArticles(authorId, q, offset, limit) ⇒
      Task.now(Articles(articles.filter{
        case (_, a) ⇒
          authorId.fold(true)(_ == a.authorId)
      }.slice(offset, offset + limit).values.toList, articles.size).asInstanceOf[A])

    case GetArticleById(i) ⇒
      Task.now(articles(i).asInstanceOf[A])
  }
}
