package mono

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import cats.~>
import monix.eval.Task
import monix.cats.MonixToCatsConversions
import mono.article._
import mono.bot._
import mono.web.WebApp

import scala.concurrent.Await
import scala.concurrent.duration._
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

  import monix.execution.Scheduler.Implicits.global

  var state: BotState = BotState.Idle

  val i: BotScript.Op ~> Task = BotConsoleInterpreter or ArticlesInMemoryInterpreter

  for (n ← 1 to 10) {
    ArticleOps.ops[BotScript.Op]
      .create("test user " + n, "title#" + n, None, Instant.now()).foldMap(i)
  }

  implicit val system = ActorSystem("mono")
  implicit val mat = ActorMaterializer ()

  val bot = new MonoBot(
    script = BotScript(),
    interpreter = _ or ArticlesInMemoryInterpreter
  )

  val web = new WebApp[ArticleOp](ArticlesInMemoryInterpreter)

  bot.run()

  web.run()

  Source.repeat(()).map(_ ⇒ Plain(StdIn.readLine(), 0l)).runWith(BotProcessor(BotScript(), i))

}

