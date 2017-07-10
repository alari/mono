package mono.core.bus

import monix.execution.Ack
import monix.reactive.{ MulticastStrategy, Observable, OverflowStrategy }
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._

case class EventBus[T](bufferSize: Int = 8, defaultTimeout: FiniteDuration = 10.seconds) extends CancelableBus[T](bufferSize, defaultTimeout) {

  private lazy val (sync, observable) =
    Observable.multicast[T](MulticastStrategy.Publish)

  /**
   * Pushes an event
   * @param event
   * @return
   */
  def emit(event: T): Ack =
    sync.onNext(event)

  /**
   * Create a subscription from the filter function
   * @param filter
   * @return
   */
  def subscribe(filter: T ⇒ Boolean, timeout: FiniteDuration = defaultTimeout): String = {
    val (subSync, subObservable) =
      Observable.multicast[T](MulticastStrategy.ReplayLimited(bufferSize, Seq.empty), OverflowStrategy.DropOld(bufferSize))

    val cancel = observable
      .filter(filter)
      .subscribe(subSync)

    register(subObservable, cancel)
  }

  /**
   * Create a subscription from the filter function
   * @param filter
   * @return
   */
  def subscribeWithId(id: String, filter: T ⇒ Boolean, timeout: FiniteDuration = defaultTimeout): Observable[T] =
    subscription(id, timeout).getOrElse{
      val (subSync, subObservable) =
        Observable.multicast[T](MulticastStrategy.ReplayLimited(bufferSize, Seq.empty), OverflowStrategy.DropOld(bufferSize))

      val cancel = observable
        .filter(filter)
        .subscribe(subSync)

      register(id, subObservable, cancel)
      subObservable
    }
}