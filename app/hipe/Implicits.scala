/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.FailureTypes.NotPossible
import hipe.core._

object Implicits {
  implicit def flattenOptRes(mr: Option[HIPEResult[Task]]): HIPEResult[Task] =
    mr.getOrElse(Left(NotPossible("Result does not contain data and cannot be flattened.")))

  implicit def resAsOpt(mr: HIPEResult[Task]): Option[Task] =
    mr.fold(
      err => None,
      task => Some(task)
    )

  implicit def optAsRes(mt: Option[Task]): HIPEResult[Task] =
    mt.map(t => Right(t)).getOrElse(Left(NotPossible("Option result was None and cannot be converted to Right[Task].")))
}