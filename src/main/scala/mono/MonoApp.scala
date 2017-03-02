package mono

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import cats.~>
import monix.cats.MonixToCatsConversions
import monix.eval.Task
import mono.bot.Incoming.{ Chat, Meta }
import mono.bot._
import mono.web.WebApp

import scala.io.StdIn
import scala.language.higherKinds

/*
как оно должно работать вообще?
- у нас есть статья. для неё -- круд.
  - добавить статью
  - предпросмотр по ссылке
  - публикация с анонсами
  - вывод списка -- заголовок, аннотация, мб картинка
  - вывод статьи -- заголовок, текст
  - редактирование -- заголовка или текста
  - поиск -- по строке, по автору
  - группировка как-нибудь
- есть пользователи. их на самом деле нет, поэтому пусть будут телеграммовские
- есть алиасы -- для проектов, пользователей, тегов-группировок
- есть проекты. несколько пользователей могут, по идее, писать в один проект.
- собираем статистику

отображение очень простое: лента, статья.
лента по пользователю. по поиску. по тегу. по проекту.


- Поля для страницы. Модель.
- Персистанс для страницы.
- Персистанс для автора.
- Пусть работает, проверить.
- Спроектировать флоу для публикации.
- Сделать и протестировать флоу.
- Поверстать страницы.
- Проекты-алиасы -- попробовать делать каналы.
- Написать мигратор из хттп-апи мирари
- Картинки: скачивать из телеграмма, мигрировать из мирари, показывать, ресайзить.
- Картинки в вёрстке.
- Аналитика/метрика в вёрстке.
- Релиз.
- Поиск в боте и в сайте.

 */

object MonoApp extends App with MonixToCatsConversions {
  println("hello mono")

  // теперь пора сделать слой хттп

  val interpret: Interpret.Op ~> Task = Interpret.inMemory

  implicit val system = ActorSystem("mono")
  implicit val mat = ActorMaterializer()

  val bot = new MonoBot(
    script = BotScript(),
    interpreter = _ or interpret
  )

  val web = new WebApp[Interpret.Op](interpret)

  bot.run()

  web.run()

  Source.repeat(())
    .map(_ ⇒ Incoming.console(StdIn.readLine()))
    .runWith(BotProcessor(BotScript(), BotConsoleInterpreter or interpret))

}

