package net.scalytica.symbiotic.postgres.docmanagement

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FSTreeRepository
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._

import scala.concurrent.ExecutionContext

class PostgresFSTreeRepository(val config: Config) extends FSTreeRepository {

  override def treePaths(from: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def tree(from: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def children(from: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???
}
