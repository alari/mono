package mono.article

import java.time.Instant

import mono.Result

case class Article(
  id:          Long,
  user:        String,
  title:       String,
  description: Option[String],
  createdAt:   Instant
) extends Result

case class Articles(values: List[Article], count: Int) extends Result
