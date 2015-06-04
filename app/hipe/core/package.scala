/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.FailureTypes.FailedOp

package object core {

  type HIPEResult[A] = Either[FailedOp, A]

  /**
   * Types indicating which steps are surrounding the current Step.
   */
  private[hipe] sealed trait SurroundingSteps

  private[hipe] case class PrevOrNext(prev: Step, next: Step) extends SurroundingSteps

  private[hipe] case class PrevOnly(prev: Step) extends SurroundingSteps

  private[hipe] case class NextOnly(next: Step) extends SurroundingSteps

}
