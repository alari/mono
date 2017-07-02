package mono.core

import cats.free.Free
import cats.{ Semigroup, ~> }
import doobie.imports.Transactor
import doobie.util.transactor.DriverManagerTransactor
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import monix.execution.Scheduler
import mono.Interpret
import org.scalatest.{ Matchers, WordSpec }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }

import scala.language.higherKinds

trait OpsSpec[C[_]] extends WordSpec with Matchers with ScalaFutures with Eventually
    with MonixToCatsConversions {

  type Command[T] = C[T]
  type Init = Transactor[Task] ⇒ Task[Int]

  implicit class FreeValue[T](free: Free[Command, T])(implicit i: Command ~> Task) {
    def unsafeValue: T = free.foldMap(i).runAsync(Scheduler.global).futureValue
  }

  protected implicit val taskFS2 = Interpret.taskFS2

  protected val xa = DriverManagerTransactor[Task](
    "org.postgresql.Driver", "jdbc:postgresql:test_mono"
  )

  protected implicit object initSemi extends Semigroup[Init] {
    override def combine(x: Init, y: Init): Init = t ⇒
      for {
        v ← x(t)
        w ← y(t)
      } yield v + w
  }

  protected def initSpec(init: Init) =
    Option(init).foreach { i ⇒
      "initialize tables" in {
        init(xa).runAsync(Scheduler.global).futureValue
      }
    }

  def buildSpec(name: String, init: Init = null)(implicit interpret: Command ~> Task)

}
