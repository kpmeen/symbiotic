package net.scalytica.symbiotic.api.repository

import net.scalytica.symbiotic.api.types.{
  FileId,
  ManagedFile,
  Path,
  SymbioticContext
}

import scala.concurrent.{ExecutionContext, Future}

trait FSTreeRepository {

  /**
   * Fetch only the Paths for the full folder tree structure, without any file
   * refs.
   *
   * @param from Folder location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treePaths(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[(FileId, Path)]]

  /**
   * This method will return the a collection of A instances , representing the
   * folder/directory structure that has been set-up in the database.
   *
   * @param from Folder location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of ManagedFile instances
   */
  def tree(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]]

  /**
   * This method will return the a collection of A instances, representing the
   * direct descendants for the given Folder.
   *
   * @param from Folder location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of ManagedFile instances
   */
  def children(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]]

}
