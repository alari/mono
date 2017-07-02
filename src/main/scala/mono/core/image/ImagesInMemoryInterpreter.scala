package mono.core.image

import java.nio.file.{ Files, Path }
import java.util.concurrent.atomic.AtomicInteger

import cats.~>
import monix.eval.Task

import scala.collection.concurrent.TrieMap

class ImagesInMemoryInterpreter extends (ImageOp ~> Task) {
  private val cache = TrieMap.empty[Int, (Image, Path)]
  private val id = new AtomicInteger(0)

  private val tmpDir = Files.createTempDirectory("images")

  override def apply[A](fa: ImageOp[A]): Task[A] = fa match {
    case StoreImage(userId, file, caption) ⇒
      Image.build(userId, file, caption).map { r ⇒
        r.right.map { im ⇒
          val image = im.copy(id = id.getAndIncrement())
          cache(image.id) = Image.storeFile(tmpDir, image, file)
          image
        }.asInstanceOf[A]
      }
    case GetImageFile(image) ⇒
      Task.now(cache(image.id)._2.asInstanceOf[A])

    case FindImage(imageId) ⇒
      Task.now(cache.get(imageId).map(_._1).asInstanceOf[A])
  }
}
