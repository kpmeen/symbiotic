/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core.dsl

import org.specs2._

class TransitionDSLParserSpec extends mutable.Specification {

/*
  1. when task is completed go to next step
  2. when task is rejected go to previous step
  3. when task is <status> go to step <step_id>
  4. when task is completed go to step 1231

  TOOD: This is a good candidate for property based testing :-)
*/


  "" should {
    "" in {
      pending("")
    }
  }

}
