package mono

import java.nio.file.Paths

import cats.data.Coproduct
import cats.~>
import doobie.util.transactor.DriverManagerTransactor
import fs2.util.Attempt
import monix.eval.Task
import monix.execution.Scheduler
import mono.alias.{ AliasInMemoryInterpreter, AliasOp }
import mono.article.{ ArticleOp, ArticlesDoobieInterpreter, ArticlesInMemoryInterpreter }
import mono.person.{ PersonOp, PersonsDoobieInterpreter, PersonsInMemoryInterpreter }
import mono.env.{ EnvConfigInterpreter, EnvOp }
import mono.image.{ ImageOp, ImagesDoobieInterpreter, ImagesInMemoryInterpreter }
import org.slf4j.LoggerFactory

object Interpret {

  private val log = LoggerFactory.getLogger(getClass)

  type Op0[A] = Coproduct[ArticleOp, PersonOp, A]

  type Op1[A] = Coproduct[EnvOp, Op0, A]

  type Op2[A] = Coproduct[ImageOp, Op1, A]

  type Op[A] = Coproduct[AliasOp, Op2, A]

  lazy val inMemory: Op ~> Task = {
    val i0: Op0 ~> Task = new ArticlesInMemoryInterpreter or new PersonsInMemoryInterpreter
    val i1: Op1 ~> Task = new EnvConfigInterpreter() or i0
    val i2: Op2 ~> Task = new ImagesInMemoryInterpreter or i1
    new AliasInMemoryInterpreter or i2
  }

  lazy val inPostgres: Op ~> Task = {
    implicit object taskCatchable extends fs2.util.Catchable[Task] with fs2.util.Suspendable[Task] {
      override def suspend[A](fa: ⇒ Task[A]): Task[A] = Task.defer(fa)

      override def fail[A](err: Throwable): Task[A] =
        Task.raiseError(err)

      override def attempt[A](fa: Task[A]): Task[Attempt[A]] =
        fa.map[Attempt[A]](a ⇒ Right(a)).onErrorHandle(Left(_))

      override def flatMap[A, B](a: Task[A])(f: (A) ⇒ Task[B]): Task[B] = a.flatMap(f)

      override def pure[A](a: A): Task[A] = Task.now(a)
    }

    val xa = DriverManagerTransactor[Task](
      "org.postgresql.Driver", "jdbc:postgresql:mono"
    )

    Task.sequence(Seq(
      PersonsDoobieInterpreter.init(xa),
      ImagesDoobieInterpreter.init(xa),
      ArticlesDoobieInterpreter.init(xa)
    )).map(ints ⇒
      log.info("Database Initialized: " + ints))
      .onErrorRecover{
        case e ⇒
          log.error("Cannot initialize database", e)
      }.runAsync(Scheduler.global)

    val i0: Op0 ~> Task = new ArticlesDoobieInterpreter(xa) or new PersonsDoobieInterpreter(xa)
    val i1: Op1 ~> Task = new EnvConfigInterpreter() or i0
    val i2: Op2 ~> Task = new ImagesDoobieInterpreter(xa, Paths.get("./images")) or i1
    new AliasInMemoryInterpreter or i2
  }
}
