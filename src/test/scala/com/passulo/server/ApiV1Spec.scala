package com.passulo.server

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.Json
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ApiV1Spec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  val logic: Logic = new Logic()

  val routes: Route = new ServerRoutes(logic).routes

  "API V1" should {
    "return a requested key" in {
      Get("/v1/key/passuloTest") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[String] shouldBe "MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I="
        entityAs[Json] shouldEqual jsonStructure(""""MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I="""") // include " !
      }
    }

    "return 404 for publicKeys if key-id isn't found" in {
      Get("/v1/key/fantasyland") ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return keys" in {
      Get("/v1/keys") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[Json] shouldEqual jsonStructure("""[
                                                   |  {
                                                   |    "keyId": "hhatworkv1",
                                                   |    "publicKey": "MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I=",
                                                   |    "allowedAssociations": ["Hamburg@Work"]
                                                   |  },
                                                   |  {
                                                   |    "keyId": "passuloTest",
                                                   |    "publicKey": "MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I=",
                                                   |    "allowedAssociations": ["Passulo"]
                                                   |  }
                                                   |]""".stripMargin)

      }
    }

    "return allowed-associations-for-key-id" in {
      Get("/v1/allowed-associations-for-key-id/passuloTest") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[Json] shouldEqual jsonStructure("""["Passulo"]""") // include " !

      }
    }

    "return 404 if key-id isn't found" in {
      Get("/v1/allowed-associations-for-key-id/fantasyland") ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

  }

  def jsonStructure(jsonString: String): Json = io.circe.parser.parse(jsonString) match {
    case Left(error)  => fail(s"Expected JSON could not be parsed: $error")
    case Right(value) => value
  }
}
