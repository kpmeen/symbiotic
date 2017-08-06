package net.scalytica.symbiotic.api.repository

import net.scalytica.symbiotic.api.types.CommandStatusTypes.CommandStatus
import net.scalytica.symbiotic.api.types._

import scala.concurrent.{ExecutionContext, Future}

trait FolderRepository {

  /**
   * Create a new virtual folder in GridFS.
   * If the folder is not defined, the method will attempt to create a root
   * folder if it does not already exist.
   *
   * @param f the folder to add
   * @return An option containing the Id of the created folder, or none if it
   *         already exists
   */
  def save(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]]

  /**
   * Get the folder with the given FolderId.
   *
   * @param folderId FolderId
   * @return An Option with the found Folder.
   */
  def get(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]]

  /**
   * Get the folder matching the given Path
   *
   * @param at Path to look for
   * @return An Option with the found Folder.
   */
  def get(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]]

  /**
   * Checks for the existence of a Folder
   *
   * @param f Folder
   * @return true if the folder exists, else false
   */
  def exists(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = exists(f.flattenPath)

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param at Path to look for
   * @return true if the folder exists, else false
   */
  def exists(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean]

  /**
   * Will attempt to identify if any path segments in the provided folders path
   * is missing. If found, a list of the missing Folders will be returned.
   *
   * @param p Path
   * @return list of missing folders
   */
  def filterMissing(p: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[List[Path]]

  /**
   * This method allows for modifying the path from one value to another.
   * Should only be used in conjunction with the appropriate checks for any
   * child nodes.
   *
   * @param orig FolderPath
   * @param mod  FolderPath
   * @return Option of Int with number of documents affected by the update
   */
  def move(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[CommandStatus[Int]]
}
