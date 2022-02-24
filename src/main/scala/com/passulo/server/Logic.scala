package com.passulo.server

import com.passulo.token.Token
import com.typesafe.scalalogging.StrictLogging

import java.security.interfaces.EdECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, Signature}
import java.util.Base64
import scala.util.Try

class Logic() extends StrictLogging {

  val knownKeys = Map(
    "hhatworkv1" -> publicKeyFrom("MCowBQYDK2VwAyEAJuHkcaByMosGmA5LJfoSbkPaJ/YZ4eICEsDwwLRtN+I=")
  )

  def parseToken(token: String, version: String): Either[String, PassInfo] =
    version match {
      case "1" => parseTokenV1(token)
      case other =>
        logger.warn(s"Called with unsupported version $other")
        parseTokenV1(token).fold(error => Left(s"Unknown Version $other: $error"), x => Right(x))
    }

  def parseTokenV1(token: String): Either[String, PassInfo] =
    for {
      tokenDecoded <- Try(Base64.getUrlDecoder.decode(token)).toOption.toRight("Cannot decode token")
      tokenProto   <- Try(Token.parseFrom(tokenDecoded)).toOption.toRight("Cannot parse token")
    } yield PassInfo.from(tokenProto)

  def verifyToken(token: String, signatureBase64: String, keyid: String): Either[String, Boolean] =
    for {
      keyForId     <- knownKeys.get(keyid).toRight(s"No Key found for keyId $keyid")
      publicKey    <- keyForId.toRight("Error instantiating public key")
      signature     = Signature.getInstance("Ed25519")
      _             = signature.initVerify(publicKey)
      tokenDecoded <- Try(Base64.getUrlDecoder.decode(token)).toOption.toRight("Cannot decode token")
      _             = signature.update(tokenDecoded)
      sigDecoded   <- Try(Base64.getUrlDecoder.decode(signatureBase64)).toOption.toRight("Cannot decode signature")
      valid         = signature.verify(sigDecoded)
    } yield valid

  def publicKeyFrom(text: String): Option[EdECPublicKey] = {
    val keyBytes   = Base64.getDecoder.decode(text)
    val spec       = new X509EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("ed25519")
    keyFactory.generatePublic(spec) match {
      case key: EdECPublicKey => Some(key)
      case _                  => None
    }
  }
}
