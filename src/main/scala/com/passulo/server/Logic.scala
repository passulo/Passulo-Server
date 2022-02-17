package com.passulo.server

import io.circe.parser.*

import java.nio.charset.StandardCharsets
import java.util
import java.util.Base64
import scala.concurrent.ExecutionContext

class Logic()(implicit ec: ExecutionContext) {

  val knownKeys = Map(
    "44424" -> List("26e1e471a072328b06980e4b25fa126e43da27f619e1e20212c0f0c0b46d37e2")
  )

  def parseToken(token: String): Either[Serializable, PassInfo] = {
    val SIGNATURE_LENGTH = 64
    val parts            = token.split("\\.")

    if (parts.length > 4) return Left("Paseto token must have 4 parts")
    if (parts(0).isEmpty) return Left("Version must be set")
    if (parts(1).isEmpty) return Left("Purpose must be set")
    if (parts(2).isEmpty) return Left("Payload cannot be empty")
    if (parts(3).isEmpty) return Left("Footer is required")

    (parts(0), parts(1), parts(2), parts(3)) match {
      case (version, purpose, payloadBase64, footerBase64) =>
        val footer = new String(Base64.getUrlDecoder.decode(footerBase64), StandardCharsets.UTF_8)

        val payload: Array[Byte] = Base64.getUrlDecoder.decode(payloadBase64.getBytes(StandardCharsets.UTF_8))
        if (payload.length < SIGNATURE_LENGTH)
          return Left(s"Payload must be at least $SIGNATURE_LENGTH bytes, is only ${payload.length}")

        val messageBytes: Array[Byte]   = util.Arrays.copyOf(payload, payload.length - SIGNATURE_LENGTH)
        val signatureBytes: Array[Byte] = util.Arrays.copyOfRange(payload, payload.length - SIGNATURE_LENGTH, payload.length)

        for {
          claims       <- decode[Map[String, String]](new String(messageBytes, StandardCharsets.UTF_8))
          footerClaims <- decode[Map[String, String]](footer)
          keyId        <- footerClaims.get("kid").toRight("No keyId in footer")
        } yield {
          println(s"""Token
                     |version $version
                     |purpose $purpose
                     |claims $claims
                     |keyid $keyId
                     |signature ${signatureBytes.mkString(" ")}
                     |""".stripMargin)
          PassInfo(claims)
        }
    }
  }
}
