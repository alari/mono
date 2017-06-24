package mono.image

import java.io.FileInputStream
import java.nio.file.{ Files, Path }
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.security.MessageDigest
import javax.imageio.ImageIO

import cats.~>
import monix.eval.Task
import com.sksamuel.scrimage.{ Image ⇒ ScrImage }

import scala.collection.concurrent.TrieMap
import scala.collection.JavaConverters._
import scala.util.Try

class ImagesInMemoryInterpreter extends (ImageOp ~> Task) {
  private val cache = TrieMap.empty[Long, (Image, Path)]
  private val id = new AtomicLong(0)

  def md5file(file: Path): String = {
    val bis = new FileInputStream(file.toFile)
    val md5 = MessageDigest.getInstance("MD5")
    var buf = new Array[Byte](262144)

    Stream.continually(bis.read(buf)).takeWhile(_ != -1).foreach(md5.update(buf, 0, _))
    md5.digest().map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
  }

  override def apply[A](fa: ImageOp[A]): Task[A] = fa match {
    case StoreImage(userId, file, caption) ⇒
      // TODO: check if this user has already uploaded this image, return it from cache

      Task.defer(Task.fromTry(Try{
        val iis = ImageIO.createImageInputStream(file.toFile)

        ImageIO.getImageReaders(iis)
          .asScala
          .toStream
          .map(_.getFormatName)
          .headOption
          .fold[Either[String, String]](Left("Image format not found"))(f ⇒ Right(f.toLowerCase))
      })).map {
        case Right(subType) ⇒
          val scri = ScrImage.fromPath(file)

          val (w, h) = scri.dimensions

          val image = Image(
            id = id.getAndIncrement(),
            hash = md5file(file),
            size = Files.size(file),
            authorId = userId,
            createdAt = Instant.now(),
            subType = subType,
            caption = caption,
            width = w,
            height = h
          )
          cache(image.id) = (image, file)

          Right[String, Image](image).asInstanceOf[A]

        case Left(err) ⇒
          Left[String, Image](err).asInstanceOf[A]
      }.onErrorRecover[A] {
        case e ⇒ Left(e.getMessage).asInstanceOf[A]
      }

    case GetImageFile(image) ⇒
      Task.now(cache(image.id)._2.asInstanceOf[A])

    case FindImage(imageId) ⇒
      Task.now(cache.get(imageId).map(_._1).asInstanceOf[A])
  }
}
