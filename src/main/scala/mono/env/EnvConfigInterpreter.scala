package mono.env

import cats.~>
import com.typesafe.config.{ Config, ConfigFactory }
import monix.eval.Task
import pdi.jwt.{ JwtAlgorithm, JwtCirce }

class EnvConfigInterpreter(conf: Config = ConfigFactory.load()) extends (EnvOp ~> Task) {
  private lazy val host = conf.getString("mono.host")
  private lazy val jwtKey = conf.getString("mono.jwt-key")
  private val algo = JwtAlgorithm.HS256

  override def apply[A](fa: EnvOp[A]): Task[A] = (fa match {
    case ReadEnvHost ⇒ Task.now(host)

    case IssueToken(claim) ⇒
      Task.evalOnce(JwtCirce.encode(claim, jwtKey, algo))

    case ParseToken(token) ⇒
      Task.evalOnce(JwtCirce.decode(token, jwtKey, Seq(algo)).toOption)
  }).asInstanceOf[Task[A]]
}
