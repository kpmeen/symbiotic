/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

object FailureTypes {

  sealed trait FailedOp {

    val msg: String
  }

  case class Unknown(msg: String = "") extends FailedOp

  case class VeryBad(msg: String = "") extends FailedOp

  case class NotPossible(msg: String = "") extends FailedOp

  case class NotAllowed(msg: String = "") extends FailedOp

  case class Incomplete(msg: String = "") extends FailedOp

  case class NotFound(msg: String = "") extends FailedOp

}
