package mono.core.bus

import java.util.UUID

import monix.eval.Task
import monix.execution.{ Cancelable, CancelableFuture }
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

// TODO: what if -> subscribe1 -> subscribe2 -> cancel1 -> should be subscribed-2, but cancel-1 launches onComplete!
class CancelableBus[T](bufferSize: Int = 8, defaultTimeout: FiniteDuration = 10.seconds) {

  private val subscriptions = TrieMap.empty[String, (Observable[T], Cancelable)]
  private val delayedCancels = TrieMap.empty[String, CancelableFuture[Unit]]

  def register(id: String, observable: Observable[T], cancelable: Cancelable, timeout: FiniteDuration = defaultTimeout): String = {
    subscriptions(id) = (observable, cancelable)

    scheduleCancel(id, timeout)

    id
  }

  def register(observable: Observable[T], cancelable: Cancelable): String =
    register(UUID.randomUUID().toString, observable, cancelable)

  /**
   * Returns a new subscription
   * @param id
   * @param timeout
   * @return
   */
  def subscription(id: String, timeout: FiniteDuration = defaultTimeout): Option[Observable[T]] = subscriptions.get(id).map(_._1).map { obs ⇒
    obs
      .doOnTerminate(_ ⇒ scheduleCancel(id, timeout))
      .doOnSubscribe{ () ⇒
        delayedCancels.remove(id).foreach(_.cancel())
      }
  }

  private def scheduleCancel(id: String, timeout: FiniteDuration): CancelableFuture[Unit] =
    delayedCancels.getOrElseUpdate(id, Task.unit.delayExecution(timeout).runAsync.map { _ ⇒
      cancel(id)
    })

  private def cancel(id: String): Unit = {
    delayedCancels.remove(id)
    subscriptions.remove(id).foreach(_._2.cancel())
  }
}
