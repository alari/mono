package mono.bot

import cats.~>
import monix.eval.Task

object BotConsoleInterpreter extends (BotOp ~> Task) {
  override def apply[A](fa: BotOp[A]): Task[A] = fa match {
    case Say(text, _) ⇒
      println(text)
      Task.now(().asInstanceOf[A])
  }
}
