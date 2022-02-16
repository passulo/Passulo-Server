import Model.{Calculation, Request, Response}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.generic.auto.*
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable
import scala.concurrent.Future

class MainSpec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  val database: Database = new Database {
    private val calculations: mutable.ArrayBuffer[Calculation] = mutable.ArrayBuffer.empty
    override def selectAll: Future[Seq[Calculation]]           = Future.successful(calculations.toSeq)
    override def insert(calculation: Model.Calculation): Future[Int] = {
      calculations.addOne(calculation)
      Future.successful(1)
    }
  }
  val logic: Logic = new Logic(database)

  val routes: Route = new ServerRoutes(logic, database).routes

  "Addition should be possible" in {
    val payload = Request("add", 12, 13)
    Post("/", payload) ~> routes ~> check {
      response.status shouldBe StatusCodes.OK
      responseAs[Response].result shouldBe 25
    }

    Get("/") ~> routes ~> check {
      response.status shouldEqual StatusCodes.OK
      responseAs[List[Calculation]].head.result shouldBe "25"
    }
  }
}
