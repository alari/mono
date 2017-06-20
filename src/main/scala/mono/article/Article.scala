package mono.article

import java.time.Instant

import scala.language.higherKinds

case class Article(
  id:       Long,
  authorId: Long,

  title:    String,
  headline: Option[String],

  createdAt: Instant,

  draft: Boolean
)

case class Articles(values: List[Article], count: Int)