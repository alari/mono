package mono

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Keep, Source }
import akka.stream.testkit.scaladsl.TestSource
import cats.free.Free
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import monix.execution.Scheduler
import mono.bot.{ BotProcessor, BotScript, Incoming }
import org.scalatest.{ Matchers, WordSpec }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

abstract class ScriptSpec extends WordSpec with Matchers with ScalaFutures with Eventually
    with MonixToCatsConversions {

  val interpreter: Interpret.Op ~> Task = Interpret.inMemory
  val botCache = new BotCacheInterpreter

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val (probe, done) = TestSource.probe[String].toMat(Flow[String]
    .map(Incoming.console(_))
    .toMat(BotProcessor(BotScript(), botCache or interpreter))(Keep.right))(Keep.both).run()

  implicit class pushString(s: String) {
    def !! = {
      probe.sendNext(s)
    }
  }

  implicit class runFree[T](free: Free[Interpret.Op, T]) {
    def eval: T = free.foldMap(interpreter).runAsync(Scheduler.global).futureValue
  }
}
