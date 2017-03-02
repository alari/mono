package mono.bot

import akka.Done
import akka.stream.scaladsl.{ Flow, Keep, Sink }
import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.util.{ Failure, Success }

object BotProcessor extends MonixToCatsConversions {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(
    script:      (BotState, Incoming) ⇒ Free[BotScript.Op, BotState],
    interpreter: (BotScript.Op ~> Task)
  ): Sink[Incoming, Future[Done]] =
    Flow[Incoming].statefulMapConcat(() ⇒ {
      val state = TrieMap.empty[Long, Task[BotState]]
        .withDefault(_ ⇒ Task.now(BotState.Idle))

      (i) ⇒ {
        if (log.isDebugEnabled) {
          log.debug(s"state = ${state(i.meta.chat.id)}, i = $i")
        }

        val up = state(i.meta.chat.id).flatMap(s ⇒
          script(s, i).foldMap(interpreter))

        state(i.meta.chat.id) = up.coeval.runTry match {
          case Success(Left(fut)) ⇒
            Task.fromFuture(fut)
          case Success(Right(v)) ⇒
            Task.now(v)
          case Failure(f) ⇒
            log.warn(s"Performing a chat action failed on $i", f)
            Task.raiseError(f)
        }
        Nil
      }
    }).toMat(Sink.ignore)(Keep.right)
}