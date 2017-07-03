package mono.bot.script

import cats.free.Free
import mono.bot.BotScript.Scenario
import mono.bot.BotState.ArticleContext
import mono.bot._
import mono.core.article.{ Article, ArticleOps }
import mono.core.image.ImageOps
import mono.core.person.PersonOps

class ArticleImageScript(implicit
  B: BotOps[BotScript.Op],
                         A:  ArticleOps[BotScript.Op],
                         Au: PersonOps[BotScript.Op],
                         Im: ImageOps[BotScript.Op]) extends Script {

  private val setCoverR = "article:([0-9]+):setCover:([0-9]+)".r
  private val removeCoverR = "article:([0-9]+):removeCover:([0-9]+)".r
  private val removeImageR = "article:([0-9]+):removeImage:([0-9]+)".r
  private val addImageR = "article:([0-9]+):addImage:([0-9]+)".r

  private def imageButtons(article: Article, image: mono.core.image.Image): Seq[Seq[Inline.Button]] =
    if (article.imageIds.contains(image.id))
      Seq(Seq(
        if (article.coverId.contains(image.id)) Inline.CallbackButton("Снять с обложки", s"article:${article.id}:removeCover:${image.id}")
        else Inline.CallbackButton("Сделать обложкой", s"article:${article.id}:setCover:${image.id}"),

        Inline.CallbackButton("Убрать", s"article:${article.id}:removeImage:${image.id}")
      ))
    else Seq(Seq(
      Inline.CallbackButton("Прикрепить обратно", s"article:${article.id}:addImage:${image.id}")
    ))

  private def imageInline(article: Article, image: mono.core.image.Image, meta: Incoming.Meta, callbackId: String = null): Free[BotScript.Op, Long] =
    B.inline(
      if (article.coverId.contains(image.id)) s"Обложка ${article.title}"
      else if (article.imageIds.contains(image.id)) s"Картинка привязана к статье ${article.title}"
      else s"Картинка отвязана от статьи ${article.title}",
      imageButtons(article, image),
      meta.chat.id, Option(callbackId).map(_ → meta.messageId).map(Right(_)) orElse Some(Left(meta.messageId))
    )

  override val scenario: Scenario = {

    case (s @ ArticleContext(id), Image(fileId, caption, m)) ⇒
      for {
        ft ← B.loadFile(fileId)
        a ← Au.ensureTelegram(m.chat.id, m.chat.title.getOrElse(m.chat.id.toString))
        i ← Im.store(a.id, ft, caption)
        _ ← i.fold(
          err ⇒
            B.reply("Не удалось сохранить картинку: " + err, m),
          im ⇒
            A.addImage(id, im.id).flatMap { a ⇒
              imageInline(a, im, m)
            }
        )
      } yield s

    case (s, InlineCallback(setCoverR(id, imageId), callbackId, m)) ⇒
      for {
        im ← Im.getById(imageId.toInt)
        article ← A.setCover(id.toInt, Some(im.id))
        _ ← imageInline(article, im, m, callbackId)
      } yield s

    case (s, InlineCallback(removeCoverR(id, imageId), callbackId, m)) ⇒
      for {
        a ← A.getById(id.toInt)
        im ← Im.getById(imageId.toInt)
        article ← if (a.coverId.contains(im.id)) A.setCover(a.id, None) else Free.pure[BotScript.Op, Article](a)
        _ ← imageInline(article, im, m, callbackId)
      } yield s

    case (s, InlineCallback(removeImageR(id, imageId), callbackId, m)) ⇒
      for {
        im ← Im.getById(imageId.toInt)
        article ← A.removeImage(id.toInt, im.id)
        _ ← imageInline(article, im, m, callbackId)
      } yield s

    case (s, InlineCallback(addImageR(id, imageId), callbackId, m)) ⇒
      for {
        im ← Im.getById(imageId.toInt)
        article ← A.addImage(id.toInt, im.id)
        _ ← imageInline(article, im, m, callbackId)
      } yield s
  }
}

object ArticleImageScript {
  def apply()(implicit
    B: BotOps[BotScript.Op],
              A:  ArticleOps[BotScript.Op],
              Au: PersonOps[BotScript.Op],
              Im: ImageOps[BotScript.Op]): Script = new ArticleImageScript()
}
