/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.party

import models.party.{Organisation, User}
import services.BaseCrudRepository

object Interfaces {

  trait UserRepository extends BaseCrudRepository[User]

  trait OrganisationRepository extends BaseCrudRepository[Organisation]

}
