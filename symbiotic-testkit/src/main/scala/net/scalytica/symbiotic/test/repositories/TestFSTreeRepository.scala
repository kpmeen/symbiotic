package net.scalytica.symbiotic.test.repositories

import net.scalytica.symbiotic.api.repository.FSTreeRepository
import net.scalytica.symbiotic.api.types.{Path, SymbioticContext}

import scala.concurrent.ExecutionContext

class TestFSTreeRepository extends FSTreeRepository {
  override def treePaths(
      from: Option[Path]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def tree(
      from: Option[Path]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???

  override def children(
      from: Option[Path]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext) = ???
}
