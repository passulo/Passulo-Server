package com.passulo.server
import com.passulo.token.Token
import com.passulo.token.Token.Gender

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDate, ZoneOffset}

case class PassInfo(
    id: String,
    firstName: String,
    middleName: String,
    lastName: String,
    gender: String,
    association: String,
    number: String,
    status: String,
    company: String,
    email: String,
    telephone: String,
    validUntil: Option[LocalDate]
) {
  def fullName: String =
    (pronoun, NoneIfEmpty(firstName), NoneIfEmpty(middleName), NoneIfEmpty(lastName)) match {
      case (_, Some(f), Some(m), Some(l)) => s"$f $m $l"
      case (_, Some(f), None, Some(l))    => s"$f $l"
      case (Some(p), None, None, Some(l)) => s"$p $l"
      case (None, None, None, Some(l))    => s"$l"
      case _                              => "<no name>"
    }

  def pronoun: Option[String] =
    gender match {
      case "m" => Some("Mr.")
      case "f" => Some("Ms.")
      case "d" => Some("Mx.")
      case _   => None
    }

  def filename: String                            = java.net.URLEncoder.encode(s"$number-$fullName", StandardCharsets.UTF_8)
  def NoneIfEmpty(string: String): Option[String] = if (string.isBlank) None else Some(string)
}

object PassInfo {
  def from(token: Token) =
    new PassInfo(
      id = token.id,
      firstName = token.firstName,
      middleName = token.middleName,
      lastName = token.lastName,
      gender = token.gender match {
        case Gender.female  => "f"
        case Gender.male    => "m"
        case Gender.diverse => "d"
        case _              => ""
      },
      association = token.association,
      number = token.number,
      status = token.status,
      company = token.company,
      email = token.email,
      telephone = token.telephone,
      validUntil = token.validUntil.map(vut => LocalDate.ofInstant(Instant.ofEpochSecond(vut.seconds), ZoneOffset.UTC))
    )

}
