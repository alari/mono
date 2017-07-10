package mono.core.bus

sealed trait EventBusOp[T]

case class EmitAuth(id: String, token: String) extends EventBusOp[Unit]

case class WaitAuth(id: String) extends EventBusOp[String]
