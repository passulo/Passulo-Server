import java.time.Instant

object Model {
  case class Request(operation: String, value1: Int, value2: Int)
  case class Response(result: Int)
  case class Calculation(timestamp: Instant, operation: String, value1: Int, value2: Int, result: String)
}
