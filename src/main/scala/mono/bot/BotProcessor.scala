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

      val messages = TrieMap.empty[Long, Task[BotState]]

      (i) ⇒ {
        if (log.isDebugEnabled) {
          log.debug(s"state = ${state(i.meta.chat.id)}, i = $i")
        }

        // TODO: вынести это в какой-то интерфейс
        val current =
          if (i.meta.isUpdate && messages.contains(i.meta.messageId)) {
            messages(i.meta.messageId)
          } else {
            val s = state(i.meta.chat.id)
            messages(i.meta.messageId) = s
            s
          }

        val up = current.flatMap(s ⇒
          script(s, i).foldMap(interpreter))

        val ran = up.coeval.runTry match {
          case Success(Left(fut)) ⇒
            Task.fromFuture(fut)
          case Success(Right(v)) ⇒
            Task.now(v)
          case Failure(f) ⇒
            log.error(s"Performing a chat action failed on $i", f)
            current
        }

        if (!i.meta.isUpdate) state(i.meta.chat.id) = ran

        Nil
      }
    }).toMat(Sink.ignore)(Keep.right)
}