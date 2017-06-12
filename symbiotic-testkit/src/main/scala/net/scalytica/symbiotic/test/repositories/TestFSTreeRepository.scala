package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.persistence.FSTreeRepository
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{Path, TransUserId}

import scala.concurrent.ExecutionContext

class TestFSTreeRepository extends FSTreeRepository {
  override def treePaths(
      from: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def tree(
      from: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???

  override def children(
      from: Option[Path]
  )(implicit uid: UserId, trans: TransUserId, ec: ExecutionContext) = ???
}
