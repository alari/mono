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
                modified_at TIMESTAMP NOT NULL,
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

  case class Filter(
      authorIds: List[Int],
      isDraft:   Boolean
  ) {
    def fr =
      fr" WHERE is_draft=$isDraft " ++ (if (authorIds.isEmpty) fr""
      else fr" AND author_ids @> $authorIds ")
  }

  def insertArticle(authorIds: NonEmptyList[Int], title: String, createdAt: Instant): ConnectionIO[Int] =
    sql"INSERT INTO articles(author_ids, title, created_at, modified_at, is_draft) VALUES($authorIds, $title, $createdAt, current_timestamp, TRUE )"
      .update.withUniqueGeneratedKeys("id")

  val selectArticle: Fragment =
    sql"SELECT id,author_ids,title,headline,cover_id,image_ids,created_at,modified_at,published_year,version,is_draft FROM articles "

  def query(filter: Filter, offset: Int, limit: Int): Query0[Article] =
    (selectArticle ++ filter.fr ++ fr" OFFSET $offset LIMIT $limit").query[Article]

  def count(filter: Filter): Query0[Int] =
    (sql"SELECT COUNT(id) FROM articles " ++ filter.fr).query[Int]

  def getArticle(id: Int): Query0[Article] =
    (selectArticle ++ fr"WHERE id=$id LIMIT 1").query[Article]

  def draftArticle(id: Int): Update0 =
    sql"UPDATE articles SET is_draft=TRUE, modified_at=current_timestamp WHERE id=$id".update

  def publishDraft(id: Int, year: Int): Update0 =
    sql"UPDATE articles SET is_draft=FALSE, published_year=COALESCE(published_year, $year), modified_at=current_timestamp WHERE id=$id".update

  def getArticleText(id: Int): Query0[Option[String]] =
    sql"SELECT text FROM articles WHERE id=$id LIMIT 1".query

  def setArticleText(id: Int, value: Option[String]): Update0 =
    sql"UPDATE articles SET text=$value, version=version+1, modified_at=current_timestamp WHERE id=$id".update

  def setCoverId(id: Int, value: Option[Int]): Update0 =
    sql"UPDATE articles SET cover_id=$value, version=version+1, modified_at=current_timestamp WHERE id=$id".update

  def update(id: Int, title: String, headline: Option[String], publishedYear: Option[Int]): Update0 =
    sql"UPDATE articles SET title = $title, headline = $headline, published_year = $publishedYear, version = version+1, modified_at=current_timestamp WHERE id=$id".update

  def addImage(id: Int, imageId: Int): Update0 = {
    val imageIds = imageId :: Nil
    sql"UPDATE articles SET image_ids = array_append(image_ids, $imageId), version=version+1, modified_at=current_timestamp WHERE id=$id AND NOT image_ids @> $imageIds".update
  }

  def removeImage(id: Int, imageId: Int): Update0 = {
    val imageIds = imageId :: Nil
    sql"UPDATE articles SET image_ids = array_remove(image_ids, $imageId), version=version+1, modified_at=current_timestamp WHERE id=$id AND image_ids @> $imageIds".update
  }

  def deleteArticle(id: Int): Update0 =
    sql"DELETE FROM articles WHERE id=$id".update

}

class ArticlesDoobieInterpreter(xa: Transactor[Task]) extends (ArticleOp ~> Task) {
  import ArticlesDoobieInterpreter._

  override def apply[A](fa: ArticleOp[A]): Task[A] = (fa match {
    case CreateArticle(user, title, createdAt) ⇒
      (for {
        id ← insertArticle(NonEmptyList.of(user), title, createdAt)
        article ← getArticle(id).unique
      } yield article).transact(xa)

    case FetchArticles(authorId, q, offset, limit) ⇒
      val filter = Filter(authorId.toList, isDraft = false)
      (for {
        drafts ← query(filter, offset, limit).list
        cnt ← count(filter).unique
      } yield Articles(drafts, cnt)).transact(xa)

    case GetArticleById(i) ⇒
      // TODO handle error
      getArticle(i).unique.transact(xa)

    case FetchDrafts(authorId, offset, limit) ⇒
      val filter = Filter(authorId :: Nil, isDraft = true)
      (for {
        drafts ← query(filter, offset, limit).list
        cnt ← count(filter).unique
      } yield Articles(drafts, cnt)).transact(xa)

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

    case AddImage(i, imageId) ⇒
      (for {
        _ ← addImage(i, imageId).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case RemoveImage(i, imageId) ⇒
      (for {
        _ ← removeImage(i, imageId).run
        a ← getArticle(i).unique
      } yield a).transact(xa)

    case DeleteArticle(i) ⇒
      deleteArticle(i).run.transact(xa).map(_ > 0)

  }).map(_.asInstanceOf[A])
}
