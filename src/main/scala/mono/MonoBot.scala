package mono

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import cats.free.Free
import cats.~>
import info.mukel.telegrambot4s.api.TelegramBot
import info.mukel.telegrambot4s.methods.{ GetUpdates, ParseMode, SendMessage }
import info.mukel.telegrambot4s.models.Update
import monix.eval.Task
import mono.bot._

import scala.concurrent.Future

class MonoBot(
  script:      (BotState, Incoming) ⇒ Free[BotScript.Op, BotState],
  interpreter: (BotOp ~> Task) ⇒ (BotScript.Op ~> Task)
)(implicit override val system: ActorSystem, override val materializer: ActorMaterializer)
    extends TelegramBot {

  override def token: String = scala.io.Source.fromResource("bot.token").getLines().mkString

  val pollingInterval: Int = 20

  private val updatesSrc: Source[Update, NotUsed] = {
    type Offset = Long
    type Updates = Seq[Update]
    type OffsetUpdates = Future[(Offset, Updates)]

    val seed: OffsetUpdates = Future.successful((0L, Seq.empty[Update]))

    val iterator = Iterator.iterate(seed) {
      _ flatMap {
        case (offset, updates) ⇒
          val maxOffset = updates.map(_.updateId).fold(offset)(_ max _)
          request(GetUpdates(Some(maxOffset + 1), timeout = Some(pollingInterval)))
            .recover {
              case e: Exception ⇒
                logger.error("GetUpdates failed", e)
                Seq.empty[Update]
            }
            .map {
              (maxOffset, _)
            }
      }
    }

    val parallelism = Runtime.getRuntime.availableProcessors()

    val updateGroups =
      Source.fromIterator(() ⇒ iterator)
        .mapAsync(parallelism)(
          _ map {
            case (_, updates) ⇒ updates
          }
        )

    updateGroups.mapConcat(_.to) // unravel groups
  }

  val botOpInt: BotOp ~> Task = new (BotOp ~> Task) {
    override def apply[A](fa: BotOp[A]): Task[A] = fa match {
      case Say(text, chatId) ⇒
        Task.fromFuture(request(
          SendMessage(
            Left(chatId),
            text,
            Some(ParseMode.Markdown),
            None,
            None,
            None,
            None
          )
        )).map(_ ⇒ ()).asInstanceOf[Task[A]]
    }
  }

  override def run(): Unit =
    updatesSrc.collect {
      case u if u.message.isDefined ⇒ u.message.get
    }.map(Incoming.telegram).to(BotProcessor(script, interpreter(botOpInt))).run()

  override def shutdown(): Future[_] = Future.successful(())
}