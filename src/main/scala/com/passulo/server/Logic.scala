package com.passulo.server

import com.passulo.token.Token
import com.typesafe.scalalogging.StrictLogging

import java.security.Signature
import java.security.interfaces.EdECPublicKey
import java.util.Base64
import scala.util.{Success, Try}

class Logic() extends StrictLogging {

  def parseToken(token: String, version: String): Either[VerificationError, PassInfo] =
    version match {
      case "1" => parseTokenV1(token)
      case other =>
        logger.warn(s"Called with unsupported version $other")
        parseTokenV1(token).fold(error => Left(UnsupportedVersion(s"Unknown Version $other: $error")), Right(_))
    }

  def parseTokenV1(token: String): Either[VerificationError, PassInfo] =
    for {
      tokenDecoded <- Try(Base64.getUrlDecoder.decode(token)).toOption.toRight(DecodingTokenFailed())
      tokenProto   <- Try(Token.parseFrom(tokenDecoded)).toOption.toRight(TokenParsingFailed())
    } yield PassInfo.from(tokenProto)

  def verifyToken(token: String, signatureBase64: String, keyid: String, association: String): Either[VerificationError, Success.type] =
    for {
      _ <- verify(token, signatureBase64, keyid)
      _ <- verifyAssociation(association, keyid)
    } yield Success

  def verify(messageBase64: String, signatureBase64: String, keyid: String): Either[VerificationError, Success.type] =
    for {
      publicKey <- Keys.shared.edECPublicKeyForId(keyid).toRight(KeyNotFound())
      valid     <- verify(messageBase64, signatureBase64, publicKey)
    } yield valid

  def verify(messageBase64: String, signatureBase64: String, publicKey: EdECPublicKey): Either[VerificationError, Success.type] =
    for {
      msgDecoded <- Try(Base64.getUrlDecoder.decode(messageBase64)).toOption.toRight(DecodingTokenFailed())
      valid      <- verify(msgDecoded, signatureBase64, publicKey)
    } yield valid

  def verify(message: Array[Byte], signatureBase64: String, publicKey: EdECPublicKey): Either[VerificationError, Success.type] =
    for {
      sigDecoded     <- Try(Base64.getUrlDecoder.decode(signatureBase64)).toOption.toRight(DecodingSignatureFailed())
      signatureValid <- Try(verify(message, sigDecoded, publicKey)).toOption.toRight(DecodingSignatureFailed())
      valid          <- Either.cond(signatureValid, Success, InvalidSignature())
    } yield valid

  private def verify(message: Array[Byte], sig: Array[Byte], publicKey: EdECPublicKey): Boolean = {
    val signature = Signature.getInstance("Ed25519")
    signature.initVerify(publicKey)
    signature.update(message)
    signature.verify(sig)
  }

  private def verifyAssociation(association: String, keyid: String): Either[VerificationError, Success.type] =
    for {
      allowedAssociations <- Keys.shared.allowedAssociationsForKeyId(keyid).toRight(KeyNotFound())
      keyIsAllowed        <- Either.cond(allowedAssociations.contains(association), Success, AssociationNotAllowed())
    } yield keyIsAllowed
}

trait VerificationError {
  def message: String
}

case class UnsupportedVersion(message: String = "The specified version is not supported")                          extends VerificationError
case class KeyNotFound(message: String = "Key not found for KeyID")                                                extends VerificationError
case class TokenParsingFailed(message: String = "The token cannot be parsed")                                      extends VerificationError
case class DecodingTokenFailed(message: String = "Cannot decode token, is it valid base64url?")                    extends VerificationError
case class DecodingSignatureFailed(message: String = "Cannot decode signature")                                    extends VerificationError
case class InvalidSignature(message: String = "The signature is not valid")                                        extends VerificationError
case class AssociationNotAllowed(message: String = "The keyId is not allowed to sign passes for this association") extends VerificationError
