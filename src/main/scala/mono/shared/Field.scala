package mono.shared

import cats.data.{ NonEmptyList, Validated }
import cats.free.Free.inject
import cats.free.{ Free, Inject }
import cats.~>

import scala.language.higherKinds

class Field[D, V, E]() {

  type Err = NonEmptyList[E]

  sealed trait Op[T]

  case class Check(input: V) extends Op[Validated[Err, V]]

  case class Get(host: D) extends Op[V]

  case class Set(host: D, value: V) extends Op[D]

  class Ops[F[_]](implicit I: Inject[Op, F]) {
    def check(input: V): Free[F, Validated[Err, V]] =
      inject[Op, F](Check(input))

    def get(host: D): Free[F, V] =
      inject[Op, F](Get(host))

    def set(host: D, value: V): Free[F, D] =
      inject[Op, F](Set(host, value))

    def checkAndSet(host: D, input: V): Free[F, Validated[Err, D]] =
      check(input).flatMap[Validated[Err, D]] {
        case Validated.Valid(value) ⇒
          set(host, value).map(Validated.Valid(_))

        case e: Validated.Invalid[Err] ⇒
          Free.pure(e)
      }
  }

  def interpreter[F[_]](
    get:   D ⇒ F[V],
    set:   (D, V) ⇒ F[D],
    check: V ⇒ F[Validated[Err, V]]
  ): (Op ~> F) = new (Op ~> F) {
    override def apply[A](fa: Op[A]): F[A] = fa match {
      case Check(input) ⇒
        check(input).asInstanceOf[F[A]]

      case Get(host) ⇒
        get(host).asInstanceOf[F[A]]

      case Set(host, value) ⇒
        set(host, value).asInstanceOf[F[A]]
    }
  }
}

