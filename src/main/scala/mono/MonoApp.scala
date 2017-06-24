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

/*
- хранение
 - сделать постгрес

- отверстать статьи и ленту

- сделать единый протокол ошибок, чтобы был ValidatedNel[новая ошибка, T] везде

- загрузка картинок к статье (с инлайн кнопкой поставить/убрать обложкой)

- ci/cd

- перенести мирари
  - проконтролировать перенос дат создания

- сделать спецблоки (ютуба хватит)

- распространение
 - сайтмап
 - дзен
 - атом
 - рсс

- предпросмотр при редактировании
  - поддержка маркдаун вообще
  - клиентская и серверная
  - жаваскрипт, ажакс
  - вообще электрон

- подписки
  - слать всё впервые опубликованное всем в боте
  - подписки на конкретных авторов

- клиент в электроне
 - список черновиков
 - список публикаций
 - редактирование только текста + метаданных

- всякие группы, каналы, сериалы, теги (их на уровне постгреса, как и поиск)
 */

object MonoApp extends App with MonixToCatsConversions {
  println("hello mono")

  implicit val interpret: Interpret.Op ~> Task = Interpret.inMemory

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

