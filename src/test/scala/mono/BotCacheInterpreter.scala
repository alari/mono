package mono

import java.util.concurrent.atomic.AtomicLong

import cats.~>
import monix.eval.Task
import mono.bot.BotOp

import scala.collection.concurrent.TrieMap

class BotCacheInterpreter extends (BotOp ~> Task) {
  val mId = new AtomicLong(0l)

  val cache = TrieMap.empty[Long, BotOp[_]]

  override def apply[A](fa: BotOp[A]): Task[A] = {
    val id = mId.getAndIncrement()
    cache(id) = fa
    Task.now(id.asInstanceOf[A])
  }
}
