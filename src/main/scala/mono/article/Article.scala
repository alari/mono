package mono.article

import java.time.Instant

import cats.data.NonEmptyList

import scala.language.higherKinds

/*
+articleBody	Text 	The actual body of the article.
+name	Text 	The name of the item.
+author	Organization  orPerson 	The author of this content or rating. Please note that author is special in that HTML 5 provides a special mechanism for indicating authorship via the rel tag. That is equivalent to this and may be used interchangeably.
+dateCreated	Date  or DateTime 	The date on which the CreativeWork was created or the item was added to a DataFeed.
+dateModified	Date  or DateTime 	The date on which the CreativeWork was most recently modified or when the item's entry was modified within a DataFeed.
+datePublished	Date 	Date of first broadcast/publication.
+headline	Text 	Headline of the article.
+version	Number  or Text 	The version of the CreativeWork embodied by a specified resource.

?wordCount	Integer 	The number of words in the text of the Article.
?description	Text 	A description of the item.

-articleSection	Text 	Articles may belong to one or more 'sections' in a magazine or newspaper, such as Sports, Lifestyle, etc.
-genre	Text  or URL 	Genre of the creative work, broadcast channel or group.
-keywords	Text 	Keywords or tags used to describe this content. Multiple entries in a keywords list are typically delimited by commas.
-locationCreated	Place 	The location where the CreativeWork was created, which may not be the same as the location depicted in the CreativeWork.
-position	Integer  or Text 	The position of an item in a series or sequence of items.
-thumbnailUrl	URL 	A thumbnail image relevant to the Thing.
-image	ImageObject  or URL 	An image of the item. This can be a URL or a fully described ImageObject.

 */

case class Article(
  id:        Int,
  authorIds: NonEmptyList[Int],

  title:    String,
  headline: Option[String],

  coverId:  Option[Int],
  imageIds: List[Int],

  createdAt:  Instant,
  modifiedAt: Instant,

  publishedYear: Option[Int],

  version: Int,

  draft: Boolean
)

case class Articles(values: List[Article], count: Int)