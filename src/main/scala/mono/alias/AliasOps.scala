package mono.alias

import cats.free.{ Free, Inject }
import Free.inject

import scala.language.higherKinds

class AliasOps[F[_]](implicit Ao: Inject[AliasOp, F]) {

  def getAlias(id: String): Free[F, Alias] =
    inject[AliasOp, F](GetAlias(id))

  def findArticleAlias(articleId: Long): Free[F, Option[Alias]] =
    inject[AliasOp, F](FindAlias(AliasPointer.Article(articleId)))

  def findAuthorAlias(authorId: Long): Free[F, Option[Alias]] =
    inject[AliasOp, F](FindAlias(AliasPointer.Author(authorId)))

  def tryPointArticleTo(id: String, articleId: Long): Free[F, Option[Alias]] =
    inject[AliasOp, F](TryPointTo(id, AliasPointer.Article(articleId)))

  def tryPointAuthorTo(id: String, authorId: Long): Free[F, Option[Alias]] =
    inject[AliasOp, F](TryPointTo(id, AliasPointer.Author(authorId)))

}

object AliasOps {
  implicit def ops[F[_]](implicit Ao: Inject[AliasOp, F]): AliasOps[F] = new AliasOps[F]
}
