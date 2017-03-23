package mono.env

import cats.~>
import com.typesafe.config.{ Config, ConfigFactory }
import monix.eval.Task

class EnvConfigInterpreter(conf: Config = ConfigFactory.load()) extends (EnvOp ~> Task) {
  private lazy val host = conf.getString("mono.host")

  override def apply[A](fa: EnvOp[A]): Task[A] = (fa match {
    case ReadEnvHost ⇒ Task.now(host)

  }).asInstanceOf[Task[A]]
}
