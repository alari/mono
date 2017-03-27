package mono.alias

sealed trait AliasOp[T]

case class TryPointTo(id: String, pointer: AliasPointer, force: Boolean) extends AliasOp[Option[Alias]]

case class FindAliases(pointers: Iterable[AliasPointer]) extends AliasOp[List[(AliasPointer, Alias)]]

case class GetAlias(id: String) extends AliasOp[Alias]