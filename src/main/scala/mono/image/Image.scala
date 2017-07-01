package mono.image

import java.io.FileInputStream
import java.nio.file.{ Files, Path, Paths }
import java.security.MessageDigest
import java.time.Instant
import javax.imageio.ImageIO

import monix.eval.Task

import scala.util.Try

case class Image(
    id:   Long,
    hash: String,
    size: Long,

    personId: Long,

    createdAt: Instant,

    subType: String,
    caption: Option[String],

    width:  Int,
    height: Int
) {
  def url: String = s"/image/$id"

  def fileDir: String = s"${personId.toString.take(2)}/$personId/${id.toString.take(2)}/$id"
  def fileName: String = s"$id.$subType"
}

object Image {
  import com.sksamuel.scrimage.{ Image ⇒ ScrImage }
  import scala.collection.JavaConverters._

  private def md5file(file: Path): String = {
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

  // TODO: use errors protocol
  def build(
    userId:  Long,
    file:    Path,
    caption: Option[String]
  ): Task[Either[String, Image]] = Task.defer(Task.fromTry(Try{
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
        id = -1,
        hash = Image.md5file(file),
        size = Files.size(file),
        personId = userId,
        createdAt = Instant.now(),
        subType = subType,
        caption = caption,
        width = w,
        height = h
      )

      Right[String, Image](image)

    case Left(err) ⇒
      Left[String, Image](err)
  }.onErrorRecover {
    case e ⇒ Left(e.getMessage)
  }

  def storeFile(baseDir: Path, image: Image, file: Path): (Image, Path) = {
    val dir = Files.createDirectories(baseDir.resolve(image.fileDir))
    val path = dir.resolve(image.fileName)
    Files.move(file, path)
    (image, path)
  }
}