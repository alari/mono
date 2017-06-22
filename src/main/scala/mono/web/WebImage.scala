package mono.web

import java.nio.file.{ Files, Path }

import akka.http.scaladsl.model.MediaType.NotCompressible
import akka.http.scaladsl.model.{ ContentType, HttpEntity, MediaType, MediaTypes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import cats.free.Free
import cats.~>
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import mono.image.{ Image, ImageOps }

import scala.language.higherKinds

class WebImage[F[_]](implicit Im: ImageOps[F]) extends Web[F] {

  override def route(implicit i: F ~> Task): Route =
    pathPrefix("image") {
      path(LongNumber) { imageId ⇒
        // TODO: send cache headers
        onSuccess((for {
          im ← Im.find(imageId)
          ip ← im match {
            case Some(img) ⇒ Im.getFile(img).map[Option[(Image, Path)]](p ⇒ Some(img → p))
            case None      ⇒ Free.pure[F, Option[(Image, Path)]](None)
          }
        } yield ip).foldMap(i).runAsync) {
          case Some((img, file)) ⇒
            complete(
              HttpEntity(
                ContentType.Binary(MediaType.image(img.subType, NotCompressible)),
                Files.size(file),
                FileIO.fromPath(file, chunkSize = 262144)
              )
            )

          case None ⇒
            // TODO: render 404 error
            complete(
              "Error"
            )

        }
      }
    }
}

