package mono.bot

sealed trait BotState

// TODO: простая сериализация и десериализация
object BotState {

  /**
   * Ждём
   * [[mono.bot.script.FetchArticlesScript.scenario]]
   * [[mono.bot.script.NewArticleScript.scenario]]
   * [[mono.bot.script.FetchDraftsScript.scenario]]
   * [[mono.bot.script.HelpScript.scenario]]
   */
  case object Idle extends BotState

  /**
   * Будем создавать статью, для этого нужен заголовок
   * [[mono.bot.script.NewArticleScript.scenario]]
   */
  case object InitNewArticle extends BotState

  /**
   * В контексте статьи
   * [[mono.bot.script.ArticleScript.scenario]]
   *
   * @param id айдишник статьи
   */
  case class ArticleContext(id: Int) extends BotState

  /**
   * В процессе листания опубликованных статей
   * [[mono.bot.script.FetchArticlesScript.scenario]]
   *
   * @param offset offset
   * @param limit limit
   * @param total total
   */
  case class FetchingArticles(offset: Int, limit: Int, total: Int) extends BotState

}
