package mono.bot.script

import cats.Monoid
import mono.bot.BotScript
import mono.bot.BotScript.Scenario

trait Script {
  val scenario: BotScript.Scenario
}

object Script {
  implicit val scriptMonoid: Monoid[Script] = new Monoid[Script] {
    override def empty: Script = new Script {
      override val scenario: Scenario = PartialFunction.empty
    }

    override def combine(x: Script, y: Script): Script = new Script {
      override val scenario: Scenario = x.scenario orElse y.scenario
    }
  }
}