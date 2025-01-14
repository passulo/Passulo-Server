package com.passulo.server

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.passulo.server.database.PassuloDB
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.Json
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MainSpec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  val logic: Logic = new Logic
  val database     = new PassuloDB

  val routes: Route = new ServerRoutes(logic, database).routes

  "Server" should {
    "be reachable" in
      Get("/") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }

    "serve Apple App Site Association with json-content-type" in
      Get("/.well-known/apple-app-site-association") ~> routes ~> check {
        contentType shouldBe ContentTypes.`application/json`
        entityAs[Json].toString() should include("applinks")
      }
  }
}
