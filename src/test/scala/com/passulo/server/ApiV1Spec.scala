package com.passulo.server

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.passulo.server.ServerRoutes.SignedPassId
import com.passulo.server.database.PassuloDB
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.Json
import io.circe.generic.auto.*
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ApiV1Spec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  val logic: Logic = new Logic()
  val database     = new PassuloDB()
  database.init

  val routes: Route = new ServerRoutes(logic, database).routes

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

    "return if a pass can be found, is valid or is not valid" in {
      val randomId = "testPassId" + System.currentTimeMillis()

      Get(s"/v1/pass/verify/$randomId") ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }

      database.register(randomId, "keyId")
      Thread.sleep(50)

      Get(s"/v1/pass/verify/$randomId") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Json] shouldEqual jsonStructure("true")
      }

      database.deactivate(randomId)
      Thread.sleep(50)

      Get(s"/v1/pass/verify/$randomId") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Json] shouldEqual jsonStructure("false")
      }
    }

    "register a new pass" in {
      val payload =
        SignedPassId("hallo", "passuloTest", "DpN2hVYdix_dYTvU7sB0gSea-KACkIm18_TE2LQBe7kBCeVQuwVU_hcOwkcsKz3eW5vBqLmOlAqC_qLccMx9Dw==")

      Post("/v1/pass/register", payload) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
      }
    }

    "fail registration if a pass is already registered" in {
      val payload =
        SignedPassId("hallo", "passuloTest", "DpN2hVYdix_dYTvU7sB0gSea-KACkIm18_TE2LQBe7kBCeVQuwVU_hcOwkcsKz3eW5vBqLmOlAqC_qLccMx9Dw==")

      Post("/v1/pass/register", payload) ~> routes ~> check {
        status shouldEqual StatusCodes.Conflict
      }
    }

    "reject registration of a new pass if the signature is invalid" in {
      val payload = SignedPassId("hallo", "passuloTest", "wrongSignature")

      Post("/v1/pass/register", payload) ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject registration of a new pass if signature is syntactically correct but not for this message" in {
      val payload =
        SignedPassId("hallo2", "passuloTest", "DpN2hVYdix_dYTvU7sB0gSea-KACkIm18_TE2LQBe7kBCeVQuwVU_hcOwkcsKz3eW5vBqLmOlAqC_qLccMx9Dw==")

      Post("/v1/pass/register", payload) ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

  }

  def jsonStructure(jsonString: String): Json = io.circe.parser.parse(jsonString) match {
    case Left(error)  => fail(s"Expected JSON could not be parsed: $error")
    case Right(value) => value
  }
}
