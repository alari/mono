package mono.api

import monix.eval.Task
import mono.core.person.Person
import sangria.schema._
import monix.execution.Scheduler.Implicits.global
import mono.core.alias.{ Alias, AliasPointer }
import mono.core.article.{ Article, Articles }
import mono.core.image.Image
import sangria.execution.deferred.{ DeferredResolver, Fetcher, HasId }

import scala.language.implicitConversions
import scala.language.higherKinds

object GraphQLSchema {

  type Ctx = GraphQLContext

  implicit val personHasId: HasId[Person, Int] = HasId(_.id)
  implicit val imageHasId: HasId[Image, Int] = HasId(_.id)
  implicit val articleHasId: HasId[Article, Int] = HasId(_.id)
  implicit val aliasHasId: HasId[Alias, AliasPointer] = HasId(_.pointer)

  val persons = Fetcher((ctx: Ctx, ids: Seq[Int]) ⇒ ctx.getPersonsByIds(ids).runAsync)
  val images = Fetcher((ctx: Ctx, ids: Seq[Int]) ⇒ ctx.getImagesByIds(ids).runAsync)
  val aliases = Fetcher((ctx: Ctx, ids: Seq[AliasPointer]) ⇒ ctx.getAliases(ids).runAsync)

  val resolver: DeferredResolver[Ctx] = DeferredResolver.fetchers(persons, images, aliases)

  implicit def taskAction[Ctx, Val](value: Task[Val]): ReduceAction[Ctx, Val] =
    value.runSyncMaybe.fold(FutureValue(_), Value(_))

  val ImageType = ObjectType(
    "Image",
    "Image uploaded by user",
    fields[Ctx, Image](
      Field("id", IntType,
        Some("The id of the person"),
        resolve = _.value.id),
      Field("url", StringType,
        Some("Relative URL to the image"),
        resolve = _.value.url),
      Field("hash", StringType,
        Some("MD5 hash of image file"),
        resolve = _.value.hash),
      Field("size", LongType,
        Some("Image size in bytes"),
        resolve = _.value.size),
      Field("person", PersonType,
        Some("Person who've uploaded this image"),
        resolve = c ⇒ persons.defer(c.value.personId)),
      Field("createdAt", LongType,
        Some("Created timestamp, in epoch millis"),
        resolve = _.value.createdAt.toEpochMilli),
      Field("subType", StringType,
        Some("image/* type, like jpeg or png"),
        resolve = _.value.subType),
      Field("caption", OptionType(StringType),
        Some("Image title or other caption"),
        resolve = _.value.caption),
      Field("width", IntType,
        Some("Image width"),
        resolve = _.value.width),
      Field("height", IntType,
        Some("Image height"),
        resolve = _.value.height)
    )
  )

  lazy val PersonType: ObjectType[Ctx, Person] = ObjectType(
    "Person",
    "Author or any other user",
    () ⇒ fields[Ctx, Person](
      Field("id", IntType,
        Some("The id of the person"),
        resolve = _.value.id),
      Field("name", StringType,
        Some("Person's name"),
        resolve = _.value.name),
      Field("createdAt", LongType,
        Some("Created timestamp, in epoch millis"),
        resolve = _.value.createdAt.toEpochMilli),
      Field("alias", OptionType(AliasType),
        Some("Person's alias, to be used in URL"),
        resolve = c ⇒ aliases.deferOpt(c.value)),
      Field("articles", ArticlesType,
        arguments = Offset :: Limit :: Nil,
        resolve = c ⇒ c.ctx.fetchArticles(Some(c.value.id), None, c arg Offset, c arg Limit))
    )
  )

  val ArticleType = ObjectType(
    "Article",
    "Article, blog post, poetry, or other textual content",
    () ⇒ fields[Ctx, Article](
      Field("id", IntType,
        Some("Article's ID"),
        resolve = _.value.id),
      Field("authors", ListType(PersonType),
        Some("Non-empty list of article authors"),
        resolve = c ⇒ persons.deferSeq(c.value.authorIds.toList)),
      Field("title", StringType,
        Some("Main title of the Article"),
        resolve = _.value.title),
      Field("headline", OptionType(StringType),
        Some("Headline or sub-caption of the Article"),
        resolve = _.value.headline),
      Field("cover", OptionType(ImageType),
        Some("Article cover image"),
        resolve = c ⇒ images.deferOpt(c.value.coverId)),
      Field("images", ListType(ImageType),
        Some("List of images that could be used within Article's content"),
        resolve = c ⇒ images.deferSeq(c.value.imageIds)),
      Field("createdAt", LongType,
        Some("Created timestamp, in epoch millis"),
        resolve = _.value.createdAt.toEpochMilli),
      Field("modifiedAt", LongType,
        Some("Last update timestamp, in epoch millis"),
        resolve = _.value.modifiedAt.toEpochMilli),
      Field("publishedYear", OptionType(IntType),
        Some("The year of first publication - for copyright"),
        resolve = _.value.publishedYear),
      Field("version", IntType,
        Some("Version number, incremented on meaningful content updates"),
        resolve = _.value.version),
      Field("isDraft", BooleanType,
        Some("Flag for drafts"),
        resolve = _.value.isDraft),
      Field("text", StringType,
        Some("Full Article's text, in markdown"),
        resolve = c ⇒ c.ctx.getArticleText(c.value)),
      Field("alias", OptionType(AliasType),
        Some("Article's alias, to be used in URL"),
        resolve = c ⇒ aliases.deferOpt(c.value))
    )
  )

  val ArticlesType = ObjectType(
    "Articles",
    "List of Articles (or drafts)",
    fields[Ctx, Articles](
      Field("values", ListType(ArticleType),
        Some("List of articles"),
        resolve = _.value.values),
      Field("count", IntType,
        Some("Count of all articles matching the request"),
        resolve = _.value.count)
    )
  )

  lazy val AliasType: ObjectType[Ctx, Alias] = ObjectType(
    "Alias",
    "Alias is a reference key in global namespace",
    () ⇒ fields[Ctx, Alias](
      Field("id", StringType,
        Some("Alias path part"),
        resolve = _.value.id),
      Field("article", OptionType(ArticleType),
        Some("Referenced Article (if it's an Article)"),
        resolve = c ⇒ c.value.pointer match {
          case AliasPointer.Article(articleId) ⇒ c.ctx.getArticleById(articleId).map(Some(_))
          case _                               ⇒ Task.now(None)
        }),
      Field("person", OptionType(PersonType),
        Some("Referenced Person (if it's a Person)"),
        resolve = c ⇒ c.value.pointer match {
          case AliasPointer.Person(personId) ⇒ persons.deferOpt(personId)
          case _                             ⇒ Task.now(None)
        })
    )
  )

  val PersonID = Argument("id", IntType, description = "Person ID")
  val ArticleID = Argument("id", IntType, description = "Article ID")
  val AliasID = Argument("id", StringType, description = "Alias ID")

  val Offset = Argument("offset", IntType, description = "Offset")
  val Limit = Argument("limit", IntType, description = "Limit")

  val Query = ObjectType(
    "Query", fields[Ctx, Unit](
      Field("person", PersonType,
        arguments = PersonID :: Nil,
        resolve = Projector((ctx, f) ⇒ ctx.ctx.getPersonById(ctx arg PersonID))),
      Field("article", ArticleType,
        arguments = ArticleID :: Nil,
        resolve = Projector((ctx, f) ⇒ ctx.ctx.getArticleById(ctx arg ArticleID))),
      Field("articles", ArticlesType,
        arguments = Offset :: Limit :: Nil,
        resolve = ctx ⇒ ctx.ctx.fetchArticles(None, None, ctx arg Offset, ctx arg Limit)),
      Field("alias", AliasType,
        arguments = AliasID :: Nil,
        resolve = c ⇒ c.ctx.getAliasById(c arg AliasID))
    )
  )

  val schema = Schema(Query)
}