import com.github.tminglei.slickpg.{array, PgCirceJsonSupport}
import slick.jdbc.PostgresProfile

// see https://github.com/tminglei/slick-pg/blob/master/addons/circe-json/src/test/scala/com/github/tminglei/slickpg/PgCirceJsonSupportSuite.scala
trait PostgresWithJsonProfile extends PostgresProfile with PgCirceJsonSupport with array.PgArrayJdbcTypes {
  override val pgjson = "jsonb"

  override val api: PGAPI = new PGAPI {}

  trait PGAPI extends super.API with JsonImplicits {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }
}

object PostgresWithJsonProfile extends PostgresWithJsonProfile
