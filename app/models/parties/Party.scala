/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

import models.core.{CompanyId, Id, UserId}

sealed trait Party[T <: Id] {
  val id: Option[T]
}

trait Organization extends Party[CompanyId]

trait Individual extends Party[UserId]