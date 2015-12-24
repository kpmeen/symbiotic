/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.security.authentication

import models.base.Password
import org.specs2.mutable.Specification

class CryptoSpec extends Specification {

  "Encrypting a password" should {
    "take a String and return a valid encrypted Password" in {
      val res = Crypto.encryptPassword("foobar")

      res.value must_!= "foobar"
      Crypto.isValidPassword("foobar", res) must beTrue
      Crypto.isValidPassword("fobar", res) must beFalse
    }

    "take a Password and return a valid encrypted Password" in {
      val res = Crypto.encryptPassword(Password("foobar"))

      res must_!= Password("foobar")
      Crypto.isValidPassword("foobar", res) must beTrue
      Crypto.isValidPassword("fobar", res) must beFalse
    }
  }

}
