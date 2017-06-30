package mono.person

import java.time.Instant

case class Person(
  id:         Long,
  telegramId: Long,

  name: String,

  createdAt: Instant
)