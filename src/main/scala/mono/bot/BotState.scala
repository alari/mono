package mono.bot

sealed trait BotState

object BotState {

  case object Idle extends BotState

  case object InitNewArticle extends BotState

  case class ArticleContext(id: Long, op: Option[String] = None) extends BotState

  case class FetchingArticles(offset: Int, limit: Int, total: Int) extends BotState

}
