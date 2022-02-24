package com.passulo.server

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`text/html`
import akka.http.scaladsl.model.{ContentTypes, MediaType, StatusCodes}
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.scalalogging.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import play.twirl.api.Html

class ServerRoutes(val logic: Logic) extends Directives {

  protected def twirlMarshaller[A <: AnyRef: Manifest](contentType: MediaType): ToEntityMarshaller[A] =
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
  implicit val twirlHtmlMarshaller: ToEntityMarshaller[Html] = twirlMarshaller[Html](`text/html`)

  def routes: Route =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        cors(CorsSettings.defaultSettings.withAllowGenericHttpRequests(true)) {
          pathEndOrSingleSlash {
            get {
              parameters("code", "v", "sig", "kid") { (code, version, signature, keyid) =>
                logic.parseToken(code, version) match {
                  case Left(errorMessage) => complete(html.error(errorMessage.message))
                  case Right(passInfo) =>
                    logic.verifyToken(code, signature, keyid, passInfo.association) match {
                      case Right(_)    => complete(html.index(passInfo, valid = true))
                      case Left(error) => complete(html.index(passInfo, valid = false, Some(error.message)))
                    }
                }
              } ~ pathEndOrSingleSlash {
                complete("Welcome")
              }
            }
          } ~ pathPrefix(".well-known") {
            path("apple-app-site-association") {
              getFromResource("apple-app-site-association")(ContentTypeResolver(_ => ContentTypes.`application/json`))
            }
          } ~ path("favicon.ico") {
            getFromResource("favicon.ico")
          } ~ path("assets" / Remaining) { file =>
            getFromResource("assets/" + file)
          } ~ pathPrefix("v1") {
            path("key" / Segment) { id: String =>
              Keys.shared.publicKeyForId(id) match {
                case Some(key) => complete(key.asJson)
                case None      => complete(StatusCodes.NotFound)
              }
            } ~
              path("keys") {
                complete(Keys.shared.allKeys.asJson)
              } ~
              path("allowed-associations-for-key-id" / Segment) { keyId: String =>
                Keys.shared.allowedAssociationsForKeyId(keyId) match {
                  case Some(names) => complete(names.asJson)
                  case None        => complete(StatusCodes.NotFound, s"No entry for key $keyId found!")
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
