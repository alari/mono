package mono.core.article

import java.time.Instant

import cats.~>
import doobie.imports.Transactor
import doobie.util.update.Update0
import monix.eval.Task
import doobie.imports._
import cats.data._
import cats.instances._
import cats.implicits._
import fs2.interop.cats._
import doobie.postgres.imports._

object ArticlesDoobieInterpreter {
  implicit private val nelMeta: Meta[NonEmptyList[Int]] =
    Meta[List[Int]].nxmap[NonEmptyList[Int]](NonEmptyList.fromListUnsafe, _.toList)

  implicit private val han = LogHandler.jdkLogHandler

  private val createArticlesTable: Update0 =
    sql"""CREATE TABLE IF NOT EXISTS articles(
                id SERIAL PRIMARY KEY,
                author_ids INTEGER[] NOT NULL,
                title VARCHAR(512) NOT NULL,
                headline TEXT,
                cover_id INTEGER REFERENCES images,
                image_ids INTEGER[] NOT NULL DEFAULT ARRAY[]::integer[],
                created_at TIMESTAMP NOT NULL,
                modified_at TIMESTAMP DEFAULT current_timestamp,
                published_year SMALLINT,
                version SMALLINT DEFAULT 0,
                is_draft BOOLEAN NOT NULL,
                text TEXT
                )""".update

  private val createArticleAuthorsIndex: Update0 =
    sql"CREATE INDEX IF NOT EXISTS articles_author_ids_idx ON articles USING GIN (author_ids)".update

  private val createArticleImagesIndex: Update0 =
    sql"CREATE INDEX IF NOT EXISTS articles_image_ids_idx ON articles USING GIN (image_ids)".update

  def init(xa: Transactor[Task]): Task[Int] =
    (createArticlesTable.run *> createArticleAuthorsIndex.run *> createArticleImagesIndex.run).transact(xa)

  def insertArticle(authorIds: NonEmptyList[Int], title: String, createdAt: Instant): ConnectionIO[Int] =
    sql"INSERT INTO articles(author_ids, title, created_at, is_draft) VALUES($authorIds, $title, $createdAt, TRUE )"
      .update.withUniqueGeneratedKeys("id")

  val selectArticle: Fragment = sql"SELECT id,author_ids,title,headline,cover_id,image_ids,created_at,modified_at,published_year,version,is_draft FROM articles "

  def queryArticles: Query0[Article] =
    (selectArticle ++ fr"WHERE is_draft=FALSE").query[Article]

  def queryDrafts(authorId: Int): Query0[Article] =
    {
      val ids = List(authorId)
      (selectArticle ++ fr"WHERE author_ids @> $ids AND is_draft=TRUE").query[Article]
    }

  def getArticle(id: Int): Query0[Article] =
    (selectArticle ++ fr"WHERE id=$id LIMIT 1").query[Article]

  def draftArticle(id: Int): Update0 =
    sql"UPDATE articles SET is_draft=TRUE WHERE id=$id".update

  def publishDraft(id: Int, year: Int): Update0 =
    sql"UPDATE articles SET is_draft=FALSE, published_year=COALESCE(published_year, $year) WHERE id=$id".update

  def getArticleText(id: Int): Query0[Option[String]] =
    sql"SELECT text FROM articles WHERE id=$id LIMIT 1".query

  def setArticleText(id: Int, value: Option[String]): Update0 =
    sql"UPDATE articles SET text=$value, version=version+1 WHERE id=$id".update

  def setTitle(id: Int, value: String): Update0 =
    sql"UPDATE articles SET title=$value, version=version+1 WHERE id=$id".update

  def setCoverId(id: Int, value: Option[Int]): Update0 =
    sql"UPDATE articles SET cover_id=$value, version=version+1 WHERE id=$id".update

  def setHeadline(id: Int, value: Option[String]): Update0 =
    sql"UPDATE articles SET headline=$value, version=version+1 WHERE id=$id".update

  def update(id: Int, title: String, headline: Option[String], publishedYear: Option[Int]): Update0 =
    sql"UPDATE articles SET title = $title, headline = $headline, published_year = $publishedYear, version = version+1 WHERE id=$id".update
}

class ArticlesDoobieInterpreter(xa: Transactor[Task]) extends (ArticleOp ~> Task) {
  import ArticlesDoobieInterpreter._

  override def apply[A](fa: ArticleOp[A]): Task[A] = (fa match {
    case CreateArticle(user, title, createdAt) ⇒
      insertArticle(NonEmptyList.of(user), title, createdAt).transact(xa).map { id ⇒
        Article(id, NonEmptyList.of(user), title, None, None, Nil, createdAt, createdAt, None, 0, isDraft = true)
      }

    case FetchArticles(authorId, q, offset, limit) ⇒
      queryArticles.to[List].transact(xa).map { pubs ⇒
        // TODO query by postgres
        val filtered = pubs.filter(a ⇒
          authorId.fold(true)(a.authorIds.toList.contains))
        Articles(filtered.slice(offset, offset + limit), filtered.size)
      }

    case GetArticleById(i) ⇒
      // TODO handle error
      getArticle(i).unique.transact(xa)

    case FetchDrafts(authorId, offset, limit) ⇒
      // TODO offset/limit with doobie
      queryDrafts(authorId).list.transact(xa).map(drafts ⇒ Articles(drafts.slice(offset, offset + limit), drafts.size))

    case PublishDraft(i, y) ⇒
      (for {
        _ ← publishDraft(i, y).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case DraftArticle(i) ⇒
      (for {
        _ ← draftArticle(i).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case GetText(i) ⇒
      getArticleText(i).unique.map(_.getOrElse("")).transact(xa)

    case SetText(i, t) ⇒
      setArticleText(i, Option(t).filter(_.nonEmpty)).run.map(_ ⇒ t).transact(xa)

    case SetTitle(i, t) ⇒
      (for {
        _ ← setTitle(i, t).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case SetHeadline(i, t) ⇒
      (for {
        _ ← setHeadline(i, t).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case SetCover(i, c) ⇒
      (for {
        _ ← setCoverId(i, c).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case UpdateArticle(i, title, headline, publishedAt) ⇒
      (for {
        _ ← update(i, title, headline, publishedAt).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

  }).map(_.asInstanceOf[A])
}
