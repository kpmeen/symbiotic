package net.scalytica.symbiotic.postgres.docmanagement

import java.util.UUID

import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FileRepository
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._

import scala.concurrent.ExecutionContext

class PostgresFileRepository(val config: Config) extends FileRepository {

  override def save(f: File)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def get(id: UUID)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def getLatest(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def move(filename: String, orig: Path, mod: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def find(filename: String, maybePath: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def findLatest(filename: String, maybePath: Option[Path])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def listFiles(path: String)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def locked(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def lock(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???

  override def unlock(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ) = ???
}
