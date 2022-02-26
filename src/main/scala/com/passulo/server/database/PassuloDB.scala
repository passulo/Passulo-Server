package com.passulo.server.database
import com.passulo.server.database.PassuloDB.PassEntry
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

object PassuloDB {
  case class PassEntry(associationKeyId: String, passId: String, active: Boolean)
}

class PassuloDB {
  // uses Slick, see https://scala-slick.org/doc/3.3.0/gettingstarted.html
  private val db = Database.forConfig("database")
  // independent config: Postgres on Production, H2 for Tests
  val dc = DatabaseConfig.forConfig[JdbcProfile]("database")
  import dc.profile.api.*
  private val passes = TableQuery[Passes]

  class Passes(tag: Tag) extends Table[PassEntry](tag, Some("passulo"), "passes") {
    def associationKeyId = column[String]("association_key_id")
    def passId           = column[String]("pass_id", O.Unique)
    def active           = column[Boolean]("active")

    def * = (associationKeyId, passId, active) <> (PassEntry.tupled, PassEntry.unapply)
  }

  def init: Future[Unit]                                   = db.run(passes.schema.create)
  def register(passId: String, keyId: String): Future[Int] = db.run(passes += PassEntry(keyId, passId, active = true))
  def deactivate(passId: String): Future[Int]              = db.run(load(passId).update(false))
  def verify(passId: String): Future[Option[Boolean]]      = db.run(load(passId).result.headOption)
  private def load(passId: String)                         = passes.filter(_.passId === passId).map(_.active)
}
