import Model.{Request, Response}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.scalalogging.*
import io.circe.syntax.*

import scala.util.{Failure, Success}

class ServerRoutes(val logic: Logic, val database: Database) extends Directives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
  import io.circe.generic.auto.*

  def routes: Route =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        cors(CorsSettings.defaultSettings.withAllowGenericHttpRequests(true)) {
          pathEndOrSingleSlash {
            get {
              onComplete(database.selectAll) {
                case Success(value)     => complete(value.asJson)
                case Failure(exception) => complete(s"Error: ${exception.getMessage}")
              }
            } ~
              post {
                entity(as[Request]) { request: Request =>
                  onComplete(logic.process(request)) {
                    case Success(Right(value)) => complete(OK, Response(value))
                    case Success(Left(error))  => complete(StatusCodes.BadRequest, error)
                    case Failure(exception) =>
                      Logger("WebServer").warn(s"Request failed with exception: ${exception.getMessage}", exception)
                      complete(StatusCodes.InternalServerError, "A technical error occurred.")
                  }
                }
              }
          }
        }
      }
    }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handleNotFound {
      extractUri { uri =>
        Logger("WebServer").info(s"Ignored request to $uri")
        complete(StatusCodes.NotFound)
      }
    }
    .handle { case any =>
      extractUri { uri =>
        Logger("WebServer").error(s"Rejected request to $uri: $any")
        complete(StatusCodes.BadRequest)
      }

    }
    .result()

  val exceptionHandler: ExceptionHandler = ExceptionHandler { case any =>
    extractUri { uri =>
      Logger("WebServer").error(s"Exception for request to $uri", any)
      complete(StatusCodes.InternalServerError)
    }
  }
}
