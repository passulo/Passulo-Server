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

  "Server should be reachable" in {
    Get("/") ~> routes ~> check {
      response.status shouldEqual StatusCodes.OK
    }
  }

  "Apple App Site Association should be available and with json-content-type" in {
    Get("/.well-known/apple-app-site-association") ~> routes ~> check {
      contentType shouldBe ContentTypes.`application/json`
      entityAs[Json].toString() should include("applinks")
    }
  }
}
