package mono.alias

sealed trait AliasOp[T]

case class TryPointTo(id: String, pointer: AliasPointer) extends AliasOp[Option[Alias]]

case class FindAlias(pointer: AliasPointer) extends AliasOp[Option[Alias]]

case class GetAlias(id: String) extends AliasOp[Alias]