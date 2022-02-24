package com.passulo.server

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.Json
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MainSpec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  val logic: Logic = new Logic()

  val routes: Route = new ServerRoutes(logic).routes

  "Server" should {
    "be reachable" in {
      Get("/") ~> routes ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }

    "serve Apple App Site Association with json-content-type" in {
      Get("/.well-known/apple-app-site-association") ~> routes ~> check {
        contentType shouldBe ContentTypes.`application/json`
        entityAs[Json].toString() should include("applinks")
      }
    }

    "decode token" in {
      Get(
        "/?code=CghsMU9GVGp1QhIESm9obhoCRi4iCUFwcGxlc2VlZCgCMgYxMjM0NTY6BlBsYXRpbkIQRGV1dHNjaGUgVGVsZWtvbUoWai5hcHBsZXNlZWRAdGVsZWtvbS5kZVIQKzQ5IDQwIDEyMzQ1My0xMloMSGFtYnVyZ0BXb3JrYgYI_5nDnQZqBgj_q4DkAw==&v=1&sig=kNjbFxpdDjqPS8Q6rfluvG2NCIxuKhfRcw4RUGtoN4w56Itv8KTSznkpSYysXDk1N5oK0Ru6Vy02JrDR98owAQ==&kid=hhatworkv1"
      ) ~> routes ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }
  }
}
