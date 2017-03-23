package mono.env

sealed trait EnvOp[T]

case object ReadEnvHost extends EnvOp[String]
