/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import org.specs2.mutable.Specification

class PathSpec extends Specification {

  "Path" should {
    "return the name of last the last path segment" in {
      val pv = Path("/root/foo/bar/fizz/")

      pv.nameOfLast must_== "fizz"
    }
  }

}
