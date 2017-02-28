package mono.bot

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Sink }
import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.collection.mutable
import scala.concurrent.Future

object BotProcessor extends MonixToCatsConversions {
  def apply(
    script:      (BotState, Incoming) ⇒ Free[BotScript.Op, BotState],
    interpreter: (BotScript.Op ~> Task)
  ): Sink[Incoming, NotUsed] =
    Flow[Incoming].statefulMapConcat(() ⇒ {
      val state = mutable.Map.empty[Long, Future[BotState]]
        .withDefault(_ ⇒ Future.successful(BotState.Idle))

      (i) ⇒ {
        state(i.chatId) = state(i.chatId).flatMap(s ⇒
          script(s, i).foldMap(interpreter).runAsync)
        Nil
      }
    }).to(Sink.ignore)
}