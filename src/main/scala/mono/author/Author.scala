package mono.author

import java.time.Instant

case class Author(
  id:         Long,
  telegramId: Long,

  title: String,

  createdAt: Instant
)