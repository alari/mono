package mono.web

import akka.http.scaladsl.server.Route
import cats.~>
import monix.eval.Task
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{ BadRequest, InternalServerError, OK }
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import mono.api.{ CharacterRepo, SchemaDefinition }
import sangria.execution.deferred.DeferredResolver
import sangria.parser.QueryParser
import sangria.execution.{ ErrorWithResolver, Executor, QueryAnalysisError }
import sangria.marshalling.circe._
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder, Json, JsonNumber }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds
import scala.util.{ Failure, Success }

object WebGraphQL {
  case class Command(query: String, operationName: Option[String], vars: Option[Json])
  implicit val decoder: Decoder[Command] = deriveDecoder[Command]

  case class Error(error: String)
  implicit val encoder: Encoder[Error] = deriveEncoder[Error]
}

class WebGraphQL[F[_]] extends Web[F] with ErrorAccumulatingCirceSupport {

  import WebGraphQL._

  override def route(implicit i: F ~> Task): Route =
    (post & path("graphql")) {
      entity(as[Command]) { command ⇒

        import command._

        QueryParser.parse(query) match {

          // query parsed successfully, time to execute it!
          case Success(queryAst) ⇒
            complete(Executor.execute(SchemaDefinition.StarWarsSchema, queryAst, new CharacterRepo,
              variables = vars.getOrElse(Json.obj()),
              operationName = operationName,
              deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters))
              .map(OK → _)
              .recover {
                case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
                case error: ErrorWithResolver  ⇒ InternalServerError → error.resolveError
              })

          // can't parse GraphQL query, return error
          case Failure(error) ⇒
            complete(BadRequest, Error(error.getMessage))
        }
      }
    }

}
