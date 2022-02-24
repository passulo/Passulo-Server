package com.passulo.server

import java.time.{Instant, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

object VCardCreator {

  def createVCard(passInfo: PassInfo): String =
    s"""BEGIN:VCARD
       |VERSION:4.0
       |PRODID:-//Passulo
       |N:${passInfo.lastName};${passInfo.firstName};${passInfo.middleName};;
       |FN:${passInfo.fullName}
       |GENDER:${passInfo.gender}
       |ORG:${passInfo.company}
       |TEL;type=CELL;type=VOICE;type=pref:${passInfo.telephone}
       |EMAIL:${passInfo.email}
       |REV:${DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX", Locale.GERMANY).withZone(ZoneOffset.UTC).format(Instant.now())}
       |END:VCARD
       |""".stripMargin
}
