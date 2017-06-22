package mono.image

import java.nio.file.Path

import cats.free.Free.inject
import cats.free.{ Free, Inject }

import scala.language.higherKinds

class ImageOps[F[_]](implicit I: Inject[ImageOp, F]) {
  def upload(
    userId: Long, file: Path, caption: Option[String]
  ): Free[F, Either[String, Image]] =
    inject[ImageOp, F](UploadImage(userId, file, caption))

  def find(imageId: Long): Free[F, Option[Image]] =
    inject[ImageOp, F](FindImage(imageId))

  def getFile(image: Image): Free[F, Path] =
    inject[ImageOp, F](GetImageFile(image))
}

object ImageOps {
  implicit def ops[F[_]](implicit I: Inject[ImageOp, F]): ImageOps[F] = new ImageOps[F]
}