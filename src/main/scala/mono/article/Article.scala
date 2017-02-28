package mono.article

import java.time.Instant

case class Article(
  id:       Long,
  authorId: Long,

  title:       String,
  description: Option[String],

  createdAt: Instant
)

case class Articles(values: List[Article], count: Int)
