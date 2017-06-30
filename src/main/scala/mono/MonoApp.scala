package mono

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import mono.bot._
import mono.web.WebApp

import scala.io.StdIn
import scala.language.higherKinds
import scala.util.Try

object MonoApp extends App with MonixToCatsConversions {
  println("hello mono")

  implicit val interpret: Interpret.Op ~> Task = Interpret.inPostgres

  implicit val system = ActorSystem("mono")
  implicit val mat = ActorMaterializer()

  Try(scala.io.Source.fromResource("bot.token").getLines().mkString).foreach(token ⇒
    new MonoBot(
      token,
      script = BotScript(),
      interpreter = _ or interpret
    ).run())

  val web = new WebApp[Interpret.Op]

  web.run()

  Source.repeat(())
    .map(_ ⇒ Incoming.console(StdIn.readLine()))
    .runWith(BotProcessor(BotScript(), BotConsoleInterpreter or interpret))

}

