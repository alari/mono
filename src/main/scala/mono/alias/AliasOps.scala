package mono.alias

import cats.free.{ Free, Inject }
import Free.inject
import mono.env.EnvOps

import scala.language.higherKinds

class AliasOps[F[_]](implicit Ao: Inject[AliasOp, F]) {

  def getAlias(id: String): Free[F, Alias] =
    inject[AliasOp, F](GetAlias(id))

  def findAlias(pointer: AliasPointer): Free[F, Option[Alias]] =
    inject[AliasOp, F](FindAliases(pointer :: Nil)).map(_.headOption.map(_._2))

  def findAliases(pointers: AliasPointer*): Free[F, Map[AliasPointer, Alias]] =
    inject[AliasOp, F](FindAliases(pointers)).map(_.toMap)

  def tryPointTo(id: String, pointer: AliasPointer, force: Boolean): Free[F, Option[Alias]] =
    inject[AliasOp, F](TryPointTo(id, pointer, force))

  def aliasHref(pointer: AliasPointer, default: ⇒ String, alias: Option[Alias] = None)(implicit E: EnvOps[F]): Free[F, String] =
    for {
      a ← if (alias.isDefined) Free.pure[F, Option[Alias]](alias) else findAlias(pointer)
      host ← E.readHost()
    } yield s"$host/${a.map(_.id).getOrElse(default)}"
}

object AliasOps {
  implicit def ops[F[_]](implicit Ao: Inject[AliasOp, F]): AliasOps[F] = new AliasOps[F]
}
