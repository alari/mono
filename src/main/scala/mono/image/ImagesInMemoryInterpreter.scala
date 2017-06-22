package mono.image

import java.io.FileInputStream
import java.nio.file.{ Files, Path }
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.security.MessageDigest

import cats.~>
import monix.eval.Task
import com.sksamuel.scrimage.{ Image ⇒ ScrImage }

import scala.collection.concurrent.TrieMap

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
    case UploadImage(userId, file, caption) ⇒
      val scri = ScrImage.fromPath(file)

      val (w, h) = scri.dimensions

      val image = Image(
        id = id.getAndIncrement(),
        hash = md5file(file),
        size = Files.size(file),
        authorId = userId,
        createdAt = Instant.now(),
        subType = "jpeg",
        caption = caption,
        width = w,
        height = h
      )
      cache(image.id) = (image, file)

      Task.now(Right[String, Image](image).asInstanceOf[A])

    case GetImageFile(image) ⇒
      Task.now(cache(image.id)._2.asInstanceOf[A])

    case FindImage(imageId) ⇒
      Task.now(cache.get(imageId).map(_._1).asInstanceOf[A])
  }
}
