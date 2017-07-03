package mono.core

import java.time.{ Instant, LocalDateTime }

import cats.data.Coproduct
import cats.~>
import monix.eval.Task
import mono.core.article._
import mono.core.person.{ PersonOp, PersonOps, PersonsDoobieInterpreter, PersonsInMemoryInterpreter }
import cats.syntax.semigroup._
import mono.core.image.ImagesDoobieInterpreter

import scala.util.Random

object ArticlesSpec {
  type Command[T] = Coproduct[PersonOp, ArticleOp, T]
}

class ArticlesSpec extends OpsSpec[ArticlesSpec.Command] {

  private val persons = implicitly[PersonOps[Command]]
  private val articles = implicitly[ArticleOps[Command]]

  buildSpec("in memory articles interpreter")(new PersonsInMemoryInterpreter or new ArticlesInMemoryInterpreter)
  buildSpec(
    "doobie articles interpreter",
    PersonsDoobieInterpreter.init _ |+| ImagesDoobieInterpreter.init |+| ArticlesDoobieInterpreter.init
  )(
      new PersonsDoobieInterpreter(xa) or new ArticlesDoobieInterpreter(xa)
    )

  override def buildSpec(name: String, init: Init)(implicit interpret: ~>[Command, Task]): Unit =
    name should {
      initSpec(init)

      "create new draft" in {
        val (a, d) = (for {
          author ← persons.ensureTelegram(Random.nextLong, "test author")
          draft ← articles.create(author.id, "test title", Instant.now())
        } yield (author, draft)).unsafeValue

        d.authorIds.head shouldBe a.id
        d.publishedYear should be('empty)
        d.isDraft shouldBe true

        articles.getById(d.id).unsafeValue shouldBe d

        articles.fetchDrafts(a.id, 0, 1).unsafeValue shouldBe Articles(d :: Nil, 1)
        articles.fetchDrafts(a.id, 1, 1).unsafeValue shouldBe Articles(Nil, 1)
        articles.fetchDrafts(-a.id, 0, 1).unsafeValue shouldBe Articles(Nil, 0)
        articles.fetch(None, None, 0, 1).unsafeValue shouldBe Articles(Nil, 0)
        articles.fetch(Some(a.id), None, 0, 1).unsafeValue shouldBe Articles(Nil, 0)

        articles.delete(d.id).unsafeValue shouldBe true
        articles.delete(d.id).unsafeValue shouldBe false
      }

      "publish and hide draft" in {
        val (a, d) = (for {
          author ← persons.ensureTelegram(Random.nextLong, "test author")
          draft ← articles.create(author.id, "test title", Instant.now())
          article ← articles.publishDraft(draft.id)
        } yield (author, article)).unsafeValue

        d.authorIds.head shouldBe a.id
        d.publishedYear should be(Some(LocalDateTime.now().getYear))
        d.isDraft shouldBe false

        articles.getById(d.id).unsafeValue shouldBe d

        articles.fetchDrafts(a.id, 0, 1).unsafeValue shouldBe Articles(Nil, 0)

        articles.fetch(None, None, 0, 1).unsafeValue shouldBe Articles(d :: Nil, 1)
        articles.fetch(Some(a.id), None, 0, 1).unsafeValue shouldBe Articles(d :: Nil, 1)
        articles.fetch(Some(-a.id), None, 0, 1).unsafeValue shouldBe Articles(Nil, 0)

        val draft = articles.draftArticle(d.id).unsafeValue

        articles.fetchDrafts(a.id, 0, 1).unsafeValue shouldBe Articles(draft :: Nil, 1)
        articles.fetch(None, None, 0, 1).unsafeValue shouldBe Articles(Nil, 0)
        articles.fetch(Some(a.id), None, 0, 1).unsafeValue shouldBe Articles(Nil, 0)
        articles.fetch(Some(-a.id), None, 0, 1).unsafeValue shouldBe Articles(Nil, 0)

        articles.delete(d.id).unsafeValue shouldBe true
      }

      "add and remove image" in {
        val (a, d) = (for {
          author ← persons.ensureTelegram(Random.nextLong, "test author")
          draft ← articles.create(author.id, "test title", Instant.now())
        } yield (author, draft)).unsafeValue

        d.imageIds should be('empty)

        articles.addImage(d.id, 88).unsafeValue.imageIds should be(88 :: Nil)
        articles.addImage(d.id, 88).unsafeValue.imageIds should be(88 :: Nil)
        articles.addImage(d.id, 89).unsafeValue.imageIds should be(88 :: 89 :: Nil)
        articles.addImage(d.id, 89).unsafeValue.imageIds should be(88 :: 89 :: Nil)
        articles.addImage(d.id, 90).unsafeValue.imageIds should be(88 :: 89 :: 90 :: Nil)
        articles.removeImage(d.id, 89).unsafeValue.imageIds should be(88 :: 90 :: Nil)
        articles.removeImage(d.id, 88).unsafeValue.imageIds should be(90 :: Nil)
        articles.removeImage(d.id, 90).unsafeValue.imageIds should be('empty)

        articles.delete(d.id).unsafeValue shouldBe true
      }
    }
}
