package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.repository.FileRepository
import net.scalytica.symbiotic.api.types._

import scala.concurrent.ExecutionContext

class TestFileRepository extends FileRepository {
  override def save(
      f: File
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def findLatestBy(
      fid: FileId
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def move(
      filename: String,
      orig: Path,
      mod: Path
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def find(
      filename: String,
      maybePath: Option[Path]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def findLatest(
      filename: String,
      maybePath: Option[Path]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def listFiles(
      path: Path
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def lock(
      fid: FileId
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def unlock(
      fid: FileId
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def editable(
      from: Path
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???
}
