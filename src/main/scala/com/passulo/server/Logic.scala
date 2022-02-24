package com.passulo.server

import com.passulo.token.Token
import com.typesafe.scalalogging.StrictLogging

import java.security.Signature
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
      _ <- verifySignature(token, signatureBase64, keyid)
      _ <- verifyAssociation(association, keyid)
    } yield Success

  private def verifySignature(token: String, signatureBase64: String, keyid: String): Either[VerificationError, Success.type] =
    for {
      publicKey      <- Keys.shared.edECPublicKeyForId(keyid).toRight(KeyNotFound())
      signature       = Signature.getInstance("Ed25519")
      _               = signature.initVerify(publicKey)
      tokenDecoded   <- Try(Base64.getUrlDecoder.decode(token)).toOption.toRight(DecodingTokenFailed())
      _               = signature.update(tokenDecoded)
      sigDecoded     <- Try(Base64.getUrlDecoder.decode(signatureBase64)).toOption.toRight(DecodingSignatureFailed())
      signatureValid <- Try(signature.verify(sigDecoded)).toOption.toRight(DecodingSignatureFailed())
      valid          <- Either.cond(signatureValid, Success, InvalidSignature())
    } yield valid

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
