package com.passulo.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.passulo.server.database.PassuloDB
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.*
import scala.util.{Failure, Success}

object Main extends StrictLogging {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[?]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val port: Int = sys.env.getOrElse("PORT", "8080").toInt
    val futureBinding = Http()
      .newServerAt("0.0.0.0", port)
      .bind(routes)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        logger.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val database = new PassuloDB
      database.init

      val logic = new Logic

      // Create routes
      val routes: ServerRoutes = new ServerRoutes(logic, database)

      // Load Keys
      val keys = Keys.shared.allKeys
      logger.info(s"Loaded ${keys.size} keys. Known key-ids: ${keys.map(_.keyId).mkString(", ")}")

      // Start server
      startHttpServer(routes.routes)(context.system)

      Behaviors.empty
    }
    logger.info("Starting ActorSystem")

    val _ = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
  }
}
