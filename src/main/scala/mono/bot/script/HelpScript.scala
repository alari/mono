package mono.bot.script
import mono.bot.{ BotOps, Command }
import mono.bot.BotScript.{ Op, Scenario }

class HelpScript(implicit B: BotOps[Op]) extends Script {
  override val scenario: Scenario = {
    case (s, Command("help", _, m)) ⇒
      for {
        _ ← B.reply(
          s"""Бот знает команды:
             |/new Создать новый черновик
             |/drafts Вывести список черновиков
             |/fetch Вывести список статей
         """.stripMargin, m
        )
      } yield s
  }
}

object HelpScript {
  def apply()(implicit B: BotOps[Op]): Script = new HelpScript()
}