package com.passulo.server

import com.passulo.server.Keys.{Key, KeyConfig}
import com.typesafe.scalalogging.StrictLogging
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.security.KeyFactory
import java.security.interfaces.EdECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class Keys(configFile: String) {
  def edECPublicKeyForId(id: String): Option[EdECPublicKey]        = keys.find(_.keyId == id).flatMap(_.edECPublicKey)
  def publicKeyForId(id: String): Option[String]                   = keys.find(_.keyId == id).map(_.publicKey)
  def allowedAssociationsForKeyId(id: String): Option[Seq[String]] = keys.find(_.keyId == id).map(_.allowedAssociations)
  def allKeys: Seq[Key]                                            = keys

  private val keys = ConfigSource.resources(configFile: String).loadOrThrow[KeyConfig].keys
}

object Keys extends StrictLogging {
  val shared = new Keys("publicKeys.conf")

  private case class KeyConfig(keys: Seq[Key])

  case class Key(keyId: String, publicKey: String, allowedAssociations: Seq[String]) {
    lazy val edECPublicKey: Option[EdECPublicKey] =
      publicKeyFrom(publicKey) match {
        case Some(value) => Some(value)
        case None        => logger.warn(s"Failed to get EcEDKey from String for keyId $keyId"); None
      }
  }

  private def publicKeyFrom(text: String): Option[EdECPublicKey] = {
    val keyBytes   = Base64.getDecoder.decode(text)
    val spec       = new X509EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("ed25519")
    keyFactory.generatePublic(spec) match {
      case key: EdECPublicKey => Some(key)
      case _                  => None
    }
  }

}
