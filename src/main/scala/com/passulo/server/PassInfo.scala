package com.passulo.server
import java.nio.charset.StandardCharsets
import java.time.LocalDate

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
  def apply(claims: Map[String, String]): PassInfo = new PassInfo(
    id = "someid",
    firstName = claims.getOrElse("fna", ""),
    middleName = claims.getOrElse("mna", ""),
    lastName = claims.getOrElse("lna", ""),
    gender = claims.getOrElse("gnd", ""),
    association = claims.getOrElse("asn", ""),
    number = claims.getOrElse("num", ""),
    status = claims.getOrElse("sts", ""),
    company = claims.getOrElse("com", ""),
    email = claims.getOrElse("eml", ""),
    telephone = claims.getOrElse("tel", ""),
    validUntil = claims.get("vut").map(LocalDate.parse)
  )

}
