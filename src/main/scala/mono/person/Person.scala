package mono.person

import java.time.Instant

case class Person(
  id:         Int,
  telegramId: Long,

  name: String,

  createdAt: Instant
)