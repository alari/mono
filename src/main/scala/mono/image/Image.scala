package mono.image

import java.time.Instant

case class Image(
    id:   Long,
    hash: String,
    size: Long,

    authorId: Long,

    createdAt: Instant,

    subType: String,
    caption: Option[String],

    width:  Int,
    height: Int
) {
  def url: String = s"/image/$id"
}
