package mono.bot.script

import java.time.Instant

import mono.bot.BotScript.Scenario
import mono.bot._
import mono.core.env.EnvOps
import mono.core.person.PersonOps
import pdi.jwt.JwtClaim

class AuthScript(implicit
  B: BotOps[BotScript.Op],
                 Au: PersonOps[BotScript.Op],
                 E:  EnvOps[BotScript.Op]) extends Script {

  override val scenario: Scenario = {

    case (state, Command("auth", _, m)) ⇒
      for {
        person ← Au.ensureTelegram(m.chat.id, m.chat.title.getOrElse("???"))
        token ← E.issueToken(JwtClaim(
          issuer = Some("bot"),
          issuedAt = Some(Instant.now().getEpochSecond),
          subject = Some(person.id.toString)
        ))
        _ ← B.reply(token, m)
      } yield state
  }
}

object AuthScript {
  def apply()(implicit
    B: BotOps[BotScript.Op],
              Au: PersonOps[BotScript.Op],
              E:  EnvOps[BotScript.Op]): Script = new AuthScript()
}