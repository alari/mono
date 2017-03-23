package mono.bot

import java.util.concurrent.atomic.AtomicLong

import cats.~>
import monix.eval.Task

object BotConsoleInterpreter extends (BotOp ~> Task) {
  val mId = new AtomicLong(0l)

  override def apply[A](fa: BotOp[A]): Task[A] = fa match {
    case Say(text, _) ⇒
      println(text)
      Task.now(mId.getAndIncrement().asInstanceOf[A])

    case Reply(text, _, forceReply) ⇒
      println((if (forceReply) Console.RED else Console.YELLOW) + "> " + Console.RESET + text)
      Task.now(mId.getAndIncrement().asInstanceOf[A])

    case Choose(text, variants, _) ⇒
      println(text)
      variants.foreach(v ⇒ println("\t" + v.mkString("\t")))
      Task.now(mId.getAndIncrement().asInstanceOf[A])
  }
}
