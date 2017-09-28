package net.scalytica.symbiotic.api.repository

import net.scalytica.symbiotic.api.types._

import scala.concurrent.{ExecutionContext, Future}

trait FileRepository extends ManagedFileRepo[File] {

  /**
   * Saves the given File in the repository. If the file doesn't already exist
   * it will be added. If the File doesn't contain a FileStream, only the
   * metadata will be saved. It's possible to add a FileStream later by creating
   * a new version of the File entry.
   *
   * @param f File to save
   * @return Option[FileId]
   */
  def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]]

  /**
   * Updates the metadata for a given File. Only the following fields are
   * allowed to update:
   *
   * <ul>
   *   <li>metadata.description</li>
   *   <li>metadata.extraAttributes</li>
   * </ul>
   *
   * Other attributes must be changed by other means.
   *
   * @param f the File with updated data
   * @return Option[FileId]
   */
  def updateMetadata(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]]

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

  /**
   * Will fully erase a File from the repository. Both metadata and physical
   * file will be irrevocably removed.
   *
   * @param fid the FileId to erase
   * @return an Either[String, Int] where the right hand side contains number of
   *         successfully erased files. Typically 1:1 with number of versions of
   *         the file.
   */
  def eraseFile(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Either[String, Int]]

}
