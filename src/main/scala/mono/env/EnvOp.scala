package mono.env

import pdi.jwt.JwtClaim

sealed trait EnvOp[T]

case object ReadEnvHost extends EnvOp[String]

case class IssueToken(claim: JwtClaim) extends EnvOp[String]

case class ParseToken(token: String) extends EnvOp[Option[JwtClaim]]