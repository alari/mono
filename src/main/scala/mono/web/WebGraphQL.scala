package mono.web

import akka.http.scaladsl.server.Route
import cats.~>
import monix.eval.Task
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{ BadRequest, InternalServerError, NotFound, OK }
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import mono.api.{ GraphQLContext, GraphQLSchema }
import sangria.parser.{ QueryParser, SyntaxError }
import sangria.execution.{ ErrorWithResolver, Executor, QueryAnalysisError }
import sangria.marshalling.circe._
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder, Json }
import mono.core.alias.AliasOps
import mono.core.article.ArticleOps
import mono.core.env.EnvOps
import mono.core.image.ImageOps
import mono.core.person.PersonOps
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import mono.core.bus.{ CancelableBus, EventBusOps }
import org.slf4j.LoggerFactory
import sangria.ast.OperationType

import scala.concurrent.duration._
import scala.language.higherKinds
import scala.util.{ Failure, Success }

object WebGraphQL {
  case class Command(query: String, operationName: Option[String], variables: Option[Json])
  implicit val decoder: Decoder[Command] = deriveDecoder[Command]

  case class Error(error: String)
  implicit val encoder: Encoder[Error] = deriveEncoder[Error]
}

class WebGraphQL[F[_]](keepAlive: FiniteDuration = 10.seconds)(implicit P: PersonOps[F], A: ArticleOps[F], I: ImageOps[F], Al: AliasOps[F], E: EnvOps[F], Eb: EventBusOps[F]) extends Web[F] with ErrorAccumulatingCirceSupport {

  private val log = LoggerFactory.getLogger(getClass)
  private val sse = new CancelableBus[ServerSentEvent]()

  import WebGraphQL._
  import WebTokenCheck.tokenUserOpt

  import GraphQLSchema.{ schema, resolver }

  val executor = Executor(schema, deferredResolver = resolver)

  private def executeQuery(command: Command, userIdOpt: Option[Int])(implicit i: F ~> Task) = {
    log.debug("Exec: {}", command)

    import command._

    val ctx = GraphQLContext[F](userIdOpt)

    QueryParser.parse(query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) ⇒
        queryAst.operationType(operationName) match {
          case Some(OperationType.Subscription) ⇒
            import sangria.execution.ExecutionScheme.Stream
            import sangria.streaming.monix._

            onComplete(executor.prepare(queryAst, ctx, (),
              operationName = operationName,
              variables = variables.getOrElse(Json.obj()))){
              case Success(preparedQuery) ⇒
                val id = sse.register(preparedQuery.execute()
                  .map(result ⇒ ServerSentEvent(data = result.noSpaces)), Cancelable.empty)

                complete(OK, Json.obj("subId" → Json.fromString(id)))

              case Failure(error: QueryAnalysisError) ⇒ complete(BadRequest, error.resolveError)
              case Failure(error: ErrorWithResolver)  ⇒ complete(InternalServerError, error.resolveError)
              case Failure(error)                     ⇒ complete(InternalServerError, Error(error.getMessage))
            }

          case _ ⇒
            complete(executor.execute(queryAst, ctx, (),
              variables = variables.getOrElse(Json.obj()),
              operationName = operationName)
              .map(OK → _)
              .recover {
                case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
                case error: ErrorWithResolver  ⇒ InternalServerError → error.resolveError
              })
        }

      // can't parse GraphQL query, return error
      case Failure(error: SyntaxError) ⇒
        complete(BadRequest, Error(error.getMessage))

      case Failure(error) ⇒
        complete(InternalServerError, Error(error.getMessage))
    }
  }

  override def route(implicit i: F ~> Task): Route =
    cors() {
      (path("graphql") & tokenUserOpt[F] & post) { userIdOpt ⇒
        entity(as[Command]) { command ⇒
          executeQuery(command, userIdOpt)
        }
      } ~ (path("graphql" / Segment) & get) { id ⇒
        sse.subscription(id) match {
          case Some(observable) ⇒
            import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
            complete(
              Source.fromPublisher[ServerSentEvent](
                observable.toReactivePublisher(global)
              ).keepAlive(keepAlive, () ⇒ ServerSentEvent(data = "", `type` = "KEEPALIVE"))
            )

          case None ⇒
            complete(NotFound, Error("Subscription not found"))

        }
      }
    }

}
