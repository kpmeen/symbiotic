package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.repository.FolderRepository
import net.scalytica.symbiotic.api.types._

import scala.concurrent.ExecutionContext

class TestFolderRepository extends FolderRepository {
  override def save(
      f: Folder
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def get(
      folderId: FolderId
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def get(
      at: Path
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def exists(
      at: Path
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def filterMissing(
      p: Path
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def move(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ) = ???
}
