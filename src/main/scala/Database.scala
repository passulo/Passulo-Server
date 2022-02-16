import Model.Calculation

import java.time.Instant
import scala.concurrent.Future

trait Database {
  def selectAll: Future[Seq[Calculation]]
  def insert(calculation: Calculation): Future[Int]
}

class PostgresDatabase extends Database {

  // uses Slick-PG https://github.com/tminglei/slick-pg
  import PostgresWithJsonProfile.api.*

  // uses Slick, see https://scala-slick.org/doc/3.3.0/gettingstarted.html
  private val db           = Database.forConfig("calculationDatabase")
  private val calculations = TableQuery[Events]

  class Events(tag: Tag) extends Table[Calculation](tag, Some("public"), "calculations") {
    def timestamp = column[Instant]("timestamp")
    def operation = column[String]("operation")
    def value1    = column[Int]("value1")
    def value2    = column[Int]("value2")
    def result    = column[String]("result")

    // Every table needs a * projection with the same type as the table's type parameter
    // Mapped table: https://scala-slick.org/doc/3.3.0/schemas.html#mapped-tables
    def * = (timestamp, operation, value1, value2, result) <> (Calculation.tupled, Calculation.unapply)
  }

  def selectAll: Future[Seq[Calculation]] = db.run(calculations.result)

  def insert(calculation: Calculation): Future[Int] = db.run(calculations += calculation)

  def init: Future[Unit] = db.run(calculations.schema.create)
}
