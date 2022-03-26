package com.passulo.server
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.MediaTypes.`text/html`
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.passulo.server.ServerRoutes.{RegisterKey, SignedPassId}
import com.passulo.server.database.PassuloDB
import com.typesafe.scalalogging.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.postgresql.util.PSQLException
import play.twirl.api.Html

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success}

object ServerRoutes {
  case class SignedPassId(passId: String, keyId: String, signature: String)
  case class RegisterKey(keyId: String, association: String, key: String)
}

class ServerRoutes(val logic: Logic, db: PassuloDB) extends Directives with StrictLogging {

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
                      case Right(_) =>
                        onComplete(db.verify(passInfo.id)) {
                          case Success(Some(valid)) => complete(html.index(passInfo, valid = valid, None, code))
                          // TODO improve visualization / explain what's going on
                          case Success(None) => complete(html.index(passInfo, valid = false, Some("Could not check validity"), code))
                          case Failure(exception) =>
                            logger.warn(s"Exception from database when checking for key '${passInfo.id}'", exception)
                            complete(html.index(passInfo, valid = false, Some("Could not check validity"), code))
                        }

                      case Left(error) => complete(html.index(passInfo, valid = false, Some(error.message), code))
                    }
                }
              } ~ pathEndOrSingleSlash {
                complete(html.welcome(Keys.shared.allKeys))
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
            path("key" / "register") {
              pathEndOrSingleSlash {
                post {
                  entity(as[RegisterKey]) { request =>
                    val baseURI  = URI.create(s"https://github.com/passulo/Passulo-Server/issues/new")
                    val template = "?template=new-public-key-on-app-passulo-com.md"
                    val assignee = "&assignees=JannikArndt"
                    val labels   = "&labels=new-public-key"
                    val title    = "&title=" + URLEncoder.encode(s"New Public Key for `${request.association}`", StandardCharsets.UTF_8)
                    val body = "&body=" + URLEncoder.encode(
                      s"""My Key-ID: `${request.keyId}`
                         |My key:
                         |```
                         |${request.key}
                         |```
                         |My association: `${request.association}`
                         |
                         |I can verify my identity in the following way: <please enter description>""".stripMargin,
                      StandardCharsets.UTF_8
                    )
                    val url = baseURI.toURL.toString + template + assignee + labels + title + body
                    complete(url)
                  }
                } ~
                  get {
                    complete(
                      "This server-instance uses GitHub Issues to add new public keys: https://github.com/passulo/Passulo-Server/issues/new"
                    )
                  }
              }
            } ~
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
              } ~
              path("vcard") {
                parameters("code", "v") { (code, version) =>
                  logic.parseToken(code, version) match {
                    case Right(passInfo) =>
                      val vcard = VCardCreator.createVCard(passInfo)
                      complete(
                        HttpResponse(entity =
                          HttpEntity(MediaTypes.`text/x-vcard`.toContentType(HttpCharsets.`UTF-8`), vcard.getBytes(StandardCharsets.UTF_8))
                        )
                      )
                    case Left(errorMessage) => complete(html.error(errorMessage.message))
                  }
                }
              } ~
              pathPrefix("pass") {
                path("register") {
                  pathEndOrSingleSlash {
                    entity(as[SignedPassId]) { request =>
                      Keys.shared.edECPublicKeyForId(request.keyId) match {
                        case None =>
                          logger.info(s"Rejected pass registration because keyId '${request.keyId}' is not known to this server.")
                          complete(
                            StatusCodes.MisdirectedRequest,
                            s"This server doesn't accept passes for keyId '${request.keyId}' because it is not registered. Please register the public key of your association first."
                          )
                        case Some(publicKey) =>
                          logic.verify(request.passId.getBytes, request.signature, publicKey) match {
                            case Left(_: DecodingSignatureFailed) =>
                              logger.info(s"Rejected pass registration: signature for keyId '${request.keyId}' could not be decoded.")
                              complete(
                                StatusCodes.BadRequest,
                                s"The signature could not be decoded. Make sure to sign the passId (${request.passId}) with your key (${request.keyId}) and attach the signature base64url encoded."
                              )
                            case Left(_: InvalidSignature) =>
                              logger.info(s"Rejected pass registration: signature for keyId '${request.keyId}' is invalid.")
                              complete(
                                StatusCodes.BadRequest,
                                s"The signature is not valid. Make sure to sign the passId (${request.passId}) with your key (${request.keyId}) and attach the signature base64url encoded."
                              )
                            case Left(error) =>
                              logger.info(s"Error during pass registration: ${error.message}")
                              complete(StatusCodes.InternalServerError, "An error occurred while verifying the message.")
                            case Right(_) =>
                              logger.debug(s"Registering pass ${request.passId} for ${request.keyId}.")
                              onComplete(db.register(request.passId, request.keyId)) {
                                case Success(1) => complete(StatusCodes.Created)
                                case Success(_) => complete(StatusCodes.InternalServerError)
                                case Failure(e: Exception)                                                       // untyped, because H2-classes are not in scope
                                    if e.getMessage.contains("duplicate key value violates unique constraint") | // posrtgres
                                      e.getMessage.contains("Unique index or primary key violation") => // h2
                                  logger.info(s"Failed to register pass for ${request.keyId}, passId '${request.passId}' already exists.")
                                  complete(
                                    StatusCodes.Conflict,
                                    s"The passId '${request.passId}' already exists. Please change the key and try again."
                                  )
                                case Failure(exception) =>
                                  logger.warn(s"Failed to register pass. Request: $request Error: ${exception.getMessage}", exception)
                                  complete(StatusCodes.InternalServerError)
                              }
                          }
                      }
                    }
                  }
                } ~
                  path("deactivate") {
                    pathEndOrSingleSlash {
                      entity(as[SignedPassId]) { request =>
                        Keys.shared.edECPublicKeyForId(request.keyId) match {
                          case None =>
                            logger.info(s"Rejected pass registration because keyId '${request.keyId}' is not known to this server.")
                            complete(
                              StatusCodes.MisdirectedRequest,
                              s"This server doesn't accept passes for keyId '${request.keyId}' because it is not registered. Please register the public key of your association first."
                            )
                          case Some(publicKey) =>
                            logic.verify(request.passId.getBytes, request.signature, publicKey) match {
                              case Left(_: DecodingSignatureFailed) =>
                                logger.info(s"Rejected pass deactivation: signature for keyId '${request.keyId}' could not be decoded.")
                                complete(
                                  StatusCodes.BadRequest,
                                  s"The signature could not be decoded. Make sure to sign the passId (${request.passId}) with your key (${request.keyId}) and attach the signature base64url encoded."
                                )
                              case Left(_: InvalidSignature) =>
                                logger.info(s"Rejected pass deactivation: signature for keyId '${request.keyId}' is invalid.")
                                complete(
                                  StatusCodes.BadRequest,
                                  s"The signature is not valid. Make sure to sign the passId (${request.passId}) with your key (${request.keyId}) and attach the signature base64url encoded."
                                )
                              case Left(error) =>
                                logger.info(s"Error during pass deactivation: ${error.message}")
                                complete(StatusCodes.InternalServerError, "An error occurred while verifying the message.")
                              case Right(_) =>
                                logger.debug(s"Deactivating pass ${request.passId} for ${request.keyId}.")
                                onComplete(db.deactivate(request.passId)) {
                                  case Success(1) => complete(StatusCodes.OK)
                                  case Success(_) => complete(StatusCodes.InternalServerError)
                                  case Failure(e: PSQLException) if e.getMessage.contains("not found") =>
                                    logger.info(s"Failed to deactivate pass for ${request.keyId}, passId '${request.passId}' not found.")
                                    complete(
                                      StatusCodes.NotFound,
                                      s"The passId '${request.passId}' was not found in the database."
                                    )
                                  case Failure(exception) =>
                                    logger.warn(s"Failed to deactivate pass. Request: $request Error: ${exception.getMessage}", exception)
                                    complete(StatusCodes.InternalServerError)
                                }
                            }
                        }
                      }
                    }
                  } ~
                  path("verify" / Segment) { passId =>
                    onComplete(db.verify(passId)) {
                      case Success(Some(value)) => complete(StatusCodes.OK, value)
                      case Success(None)        => complete(StatusCodes.NotFound)
                      case Failure(exception) =>
                        logger.warn(s"Failed to verify pass ('$passId'). Error: ${exception.getMessage}", exception)
                        complete(StatusCodes.InternalServerError)
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
