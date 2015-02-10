/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.authorization


// TODO: This must be re-visited.

trait Role

sealed trait Admin extends Role

sealed trait UserRole extends Role

case class Sysadmin() extends Admin

case class CustomerAdmin() extends Admin

case class ProjectAdmin() extends Admin

case class Mediator() extends UserRole

case class Performer() extends UserRole

case class Participant() extends UserRole
