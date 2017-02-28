package mono.bot

sealed trait BotState

object BotState {

  case object Idle extends BotState

  case class Fetching(offset: Int, limit: Int, total: Int) extends BotState

  case class CreateArticleWaitDescription(title: String) extends BotState

}
