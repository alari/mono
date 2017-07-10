package mono.core

import monix.cats.MonixToCatsConversions
import mono.core.bus.EventBus
import org.scalatest.{ Matchers, WordSpec }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{ MulticastStrategy, Observable }

class EventBusSpec extends WordSpec with Matchers with ScalaFutures with Eventually
    with MonixToCatsConversions {

  "event bus" should {
    "pub observe" in {
      val (sync, pub) = Observable.multicast[String](MulticastStrategy.Publish)
      val sub = pub.headL.runAsync

      sync.onNext("z")

      sub.futureValue shouldBe "z"
    }

    "pub sub" in {

      val bus = EventBus[(String, String)]()

      val sub = bus.subscribeWithId("test", _._1 == "test")

      val f = sub.headL.runAsync

      bus.emit("some" → "shit")

      bus.emit("test" → "cozy")

      f.futureValue shouldBe ("test" → "cozy")

    }
  }

}
