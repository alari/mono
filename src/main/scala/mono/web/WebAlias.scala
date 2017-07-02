package mono.web

import akka.http.scaladsl.server.{ Directive1, Directives, Route }
import cats.~>
import monix.eval.Task
import mono.core.alias.{ Alias, AliasOps, AliasPointer }
import mono.core.article.ArticleOps
import mono.core.person.PersonOps
import Directives._
import monix.execution.Scheduler.Implicits.global
import mono.core.image.ImageOps

import scala.language.higherKinds
import scala.util.{ Failure, Success }

class WebAlias[F[_]](implicit A: ArticleOps[F], Au: PersonOps[F], Im: ImageOps[F], As: AliasOps[F]) extends Web[F] {

  def resolveAlias(implicit i: F ~> Task): Directive1[Alias] =
    path(Segment).flatMap(id ⇒
      onComplete(As.getAlias(id).foldMap(i).runAsync).flatMap {
        case Success(a) ⇒ provide(a)
        case Failure(_) ⇒ reject
      })

  override def route(implicit i: F ~> Task): Route = resolveAlias(i){ alias ⇒
    alias.pointer match {
      case AliasPointer.Article(id) ⇒
        WebArticle.articleHtml[F](id)

      case AliasPointer.Person(id) ⇒
        parameters('offset.as[Int] ? 0, 'limit.as[Int] ? 10, 'q.?) { (o, l, q) ⇒
          WebArticle.articlesHtml[F](o, l, Some(id), q)
        }
    }
  }

}
