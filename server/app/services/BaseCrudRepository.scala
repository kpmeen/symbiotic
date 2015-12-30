/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services

import core.lib.SuccessOrFailure
import models.base.Id

trait BaseCrudRepository[A] {

  // Should perform an upsert.
  def save(a: A): SuccessOrFailure

  def findById[ID <: Id](id: ID): Option[A]

  def findBy[B](b: B): Option[A]

  def listBy[B](b: B): Seq[A]

}
