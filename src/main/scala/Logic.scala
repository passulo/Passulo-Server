import Model.*

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class Logic(database: Database)(implicit ec: ExecutionContext) {

  def process(request: Request): Future[Either[String, Int]] =
    for {
      result <- Future.successful(calculate(request))
      _      <- writeToDatabase(request, result)
    } yield result

  def calculate(request: Request): Either[String, Int] =
    request match {
      case Request("add", value1, value2)      => Right(value1 + value2)
      case Request("multiply", value1, value2) => Right(value1 * value2)
      case _                                   => Left("Not suported")
    }

  def writeToDatabase(request: Request, result: Either[String, Int]): Future[AnyVal] =
    result match {
      case Right(res)  => database.insert(Calculation(Instant.now, request.operation, request.value1, request.value2, res.toString))
      case Left(error) => database.insert(Calculation(Instant.now, request.operation, request.value1, request.value2, error))
    }
}
