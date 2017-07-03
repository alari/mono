package mono.core.image

import java.nio.file.Path

sealed trait ImageOp[T]

case class StoreImage(
  userId: Int, file: Path, caption: Option[String]
) extends ImageOp[Either[String, Image]]

case class FindImage(imageId: Int) extends ImageOp[Option[Image]]

case class GetImageById(imageId: Int) extends ImageOp[Image]

case class GetImageFile(image: Image) extends ImageOp[Path]