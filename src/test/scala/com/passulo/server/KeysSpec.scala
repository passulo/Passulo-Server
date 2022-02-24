package com.passulo.server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.math.BigInteger

class KeysSpec extends AnyWordSpec with ScalatestRouteTest with Matchers with OptionValues {

  "Keys" should {
    "load the keys from the config" in {
      val keys = new Keys("testKeys.conf")

      keys.publicKeyForId("passuloTest") shouldBe defined
      keys.edECPublicKeyForId("passuloTest") shouldBe defined
      keys.edECPublicKeyForId("passuloTest").value.getPoint.getY shouldBe new BigInteger(
        "44424592913184116376246194238513212163723781984295331222540911601851264459046"
      )
      keys.allowedAssociationsForKeyId("passuloTest").size shouldBe 1
      keys.allowedAssociationsForKeyId("passuloTest").head shouldBe "Passulo"

    }
  }
}
