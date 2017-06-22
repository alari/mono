package mono.image

import java.nio.file.Path

sealed trait ImageOp[T]

case class UploadImage(
  userId: Long, file: Path, caption: Option[String]
) extends ImageOp[Either[String, Image]]

case class FindImage(imageId: Long) extends ImageOp[Option[Image]]

case class GetImageFile(image: Image) extends ImageOp[Path]