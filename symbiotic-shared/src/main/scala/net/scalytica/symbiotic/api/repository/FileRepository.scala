package net.scalytica.symbiotic.api.repository

import net.scalytica.symbiotic.api.types._

import scala.concurrent.{ExecutionContext, Future}

trait FileRepository extends ManagedFileRepo[File] {

  /**
   * Saves the given File in the repository. If the file doesn't already exist
   * it will be added, otherwise it will update the metadata. On update the
   * actual file is left unchanged.
   *
   * If the File doesn't contain a FileStream, only the metadata will be saved.
   * It's possible to add a FileStream later by creating a new version of the
   * File entry.
   *
   * @param f File to save
   * @return Option[FileId]
   */
  def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]]

  /**
   * "Moves" a file (including all versions) from one folder to another.
   *
   * @param filename String
   * @param orig     Folder
   * @param mod      Folder
   * @return An Option with the updated File
   */
  def move(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]]

  /**
   * Will return a collection of File (if found) with the provided filename and
   * folder properties.
   *
   * @param filename  String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def find(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]]

  /**
   * Search for the latest version of a file matching the provided parameters.
   *
   * @param filename  String
   * @param maybePath Option[Folder]
   * @return An Option containing the latest version of the File
   */
  def findLatest(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]]

  /**
   * List all the files in the given Folder path
   *
   * @param path String
   * @return Option[File]
   */
  def listFiles(path: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]]

}
