/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.project

import core.mongodb.{DefaultDB, WithMongoIndex}

object MembershipService extends DefaultDB with WithMongoIndex {

  override val collectionName: String = "project_memberships"

  override def ensureIndex(): Unit = ???
}
