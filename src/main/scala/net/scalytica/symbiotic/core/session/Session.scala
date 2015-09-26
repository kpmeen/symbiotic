/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.core.session

import org.scalajs.dom.localStorage

object Session {

  val sessionKey = "SYMBIOTIC_USER"

  val storage = localStorage

}
