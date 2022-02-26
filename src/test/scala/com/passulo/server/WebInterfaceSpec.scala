package com.passulo.server

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Sink
import com.passulo.server.database.PassuloDB
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WebInterfaceSpec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  val logic: Logic = new Logic()
  val database     = new PassuloDB()

  val routes: Route = new ServerRoutes(logic, database).routes

  val validToken =
    "CghsMU9GVGp1QhIESm9obhoCRi4iCUFwcGxlc2VlZCgCMgYxMjM0NTY6BlBsYXRpbkIQRGV1dHNjaGUgVGVsZWtvbUoWai5hcHBsZXNlZWRAdGVsZWtvbS5kZVIQKzQ5IDQwIDEyMzQ1My0xMloMSGFtYnVyZ0BXb3JrYgYI_5nDnQZqBgj_q4DkAw=="
  val validSignature = "kNjbFxpdDjqPS8Q6rfluvG2NCIxuKhfRcw4RUGtoN4w56Itv8KTSznkpSYysXDk1N5oK0Ru6Vy02JrDR98owAQ=="

  "The web interface" should {
    "be reachable" in {
      Get("/") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "decode token" in {
      Get(s"/?code=$validToken&v=1&sig=$validSignature&kid=hhatworkv1") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "report unknown Key IDs" in {
      Get(s"/?code=$validToken&v=1&sig=$validSignature&kid=fantasyland") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        htmlFrom(response) should include(KeyNotFound().message)
      }
    }

    "report if the key is not allowed for this association" in {
      Get(s"/?code=$validToken&v=1&sig=$validSignature&kid=passuloTest") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        htmlFrom(response) should include(AssociationNotAllowed().message)
      }
    }

    "report (syntactically) invalid signatures" in {
      Get(s"/?code=$validToken&v=1&sig=foobar&kid=hhatworkv1") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        htmlFrom(response) should include(DecodingSignatureFailed().message)
      }
    }

    "report (semantically) wrong signatures" in {
      val otherSignature = "DoTZiAlNi83yqJIj_eM7ohnxMG8lrWWwCdqLOTYC7MUkC9nI4Damgwhvd_9BVTZlaF34In9kKEt3AgvvTLlLDg=="

      Get(s"/?code=$validToken&v=1&sig=$otherSignature&kid=hhatworkv1") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        htmlFrom(response) should include(InvalidSignature().message)
      }
    }

  }

  def htmlFrom(response: HttpResponse): String = Await.result(response.entity.dataBytes.runWith(Sink.head), 1.second).utf8String
}
