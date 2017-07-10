package mono.bot.script

import java.time.Instant

import mono.bot.BotScript.Scenario
import mono.bot._
import mono.core.bus.EventBusOps
import mono.core.env.EnvOps
import mono.core.person.PersonOps
import pdi.jwt.JwtClaim

class AuthScript(implicit
  B: BotOps[BotScript.Op],
                 Au: PersonOps[BotScript.Op],
                 E:  EnvOps[BotScript.Op],
                 Eb: EventBusOps[BotScript.Op]) extends Script {

  override val scenario: Scenario = {

    case (state, Command("auth", idOpt, m)) ⇒
      for {
        person ← Au.ensureTelegram(m.chat.id, m.chat.title.getOrElse("???"))
        token ← E.issueToken(JwtClaim(
          issuer = Some("bot"),
          issuedAt = Some(Instant.now().getEpochSecond),
          subject = Some(person.id.toString)
        ))
        _ ← idOpt.fold(B.reply(token, m).map(_ ⇒ ()))(id ⇒ Eb.emitAuth(id, token))
      } yield state
  }
}

object AuthScript {
  def apply()(implicit
    B: BotOps[BotScript.Op],
              Au: PersonOps[BotScript.Op],
              E:  EnvOps[BotScript.Op],
              Eb: EventBusOps[BotScript.Op]): Script = new AuthScript()
}