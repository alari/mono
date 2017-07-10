package mono.core.bus

import cats.~>
import monix.eval.Task
import org.slf4j.LoggerFactory

class EventBusInterpreter extends (EventBusOp ~> Task) {

  private val auth = new EventBus[(String, String)]()
  private val log = LoggerFactory.getLogger(getClass)

  override def apply[A](fa: EventBusOp[A]): Task[A] = fa match {
    case EmitAuth(id, token) ⇒
      log.info(s"EmitAuth: $id -> $token")
      auth.emit(id → token)
      Task.unit.asInstanceOf[Task[A]]

    case WaitAuth(id) ⇒
      log.info(s"Wait: $id")
      auth.subscription(auth.subscribe(_._1 == id))
        .fold(Task.raiseError[A](new NoSuchElementException("Subscription not found")))(s ⇒
          s.doAfterTerminate(et ⇒ log.info("S terminate " + et))
            .doOnSubscribe(() ⇒ log.info("Subscribed to " + id))

            .headL.map(_._2.asInstanceOf[A])).doOnFinish{
          e ⇒
            log.info("Task removed: " + e)
            Task.unit
        }
  }
}
