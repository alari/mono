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

/*
- все поля редактировать

- сделать персону по схеме.орг
 - редактировать настройки персоны

 - закачивать аватарку
- сделать обложки

- сделать спецблоки (ютуба хватит)

- хранение
 - сделать постгрес

- отверстать статьи и ленту

- ci/cd

- перенести мирари

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

  val bot = new MonoBot(
    script = BotScript(),
    interpreter = _ or interpret
  )

  val web = new WebApp[Interpret.Op]

  bot.run()

  web.run()

  Source.repeat(())
    .map(_ ⇒ Incoming.console(StdIn.readLine()))
    .runWith(BotProcessor(BotScript(), BotConsoleInterpreter or interpret))

}

