package net.scalytica.symbiotic.api.persistence

import java.util.UUID

import net.scalytica.symbiotic.api.types.CommandStatusTypes.CommandStatus
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait RepositoryProvider {
  def fileRepository: FileRepository

  def folderRepository: FolderRepository

  def fsTreeRepository: FSTreeRepository
}

trait FileRepository {

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param f File
   * @return Option[FileId]
   */
  def save(f: File)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]]

  /**
   * Get the latest version of the File with the given FileId.
   *
   * @param fid FolderId
   * @return An Option with the found File.
   */
  def getLatest(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[File]]

  /**
   * List all the files in the given Folder path
   *
   * @param path String
   * @return Option[File]
   */
  def listFiles(path: Path)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[File]]

  /**
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return an Option with the UserId of the user holding the lock
   */
  def locked(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[UserId]] = {
    getLatest(fid).map(_.flatMap(fw => fw.metadata.lock.map(l => l.by)))
  }

  protected def lockFile(
      fid: FileId
  )(f: (UUID, Lock) => Future[LockOpStatus[_ <: Option[Lock]]])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] =
    lockedAnd(uid, fid) {
      case (maybeUid, dbId) =>
        maybeUid
          .map(lockedBy => Future.successful(Locked(lockedBy)))
          .getOrElse {
            f(dbId, Lock(uid, DateTime.now())).recover {
              case NonFatal(e) =>
                LockError(s"Error trying to lock $fid: ${e.getMessage}")
            }
          }
    }.map(_.getOrElse(LockError(s"File $fid was not found")))

  protected def unlockFile(
      fid: FileId
  )(f: UUID => Future[LockOpStatus[_ <: String]])(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] =
    lockedAnd(uid, fid) {
      case (maybeUid, dbId) =>
        maybeUid.fold[Future[LockOpStatus[_ <: String]]](
          Future.successful(NotLocked())
        ) {
          case usrId: UserId if uid.value == usrId.value =>
            f(dbId).recover {
              case NonFatal(e) =>
                LockError(s"Error trying to unlock $fid: ${e.getMessage}")
            }
          case _ =>
            Future.successful(NotAllowed())
        }
    }.map(_.getOrElse(LockError(s"File $fid was not found")))

  protected def lockedAnd[A](uid: UserId, fid: FileId)(
      f: (Option[UserId], UUID) => Future[A]
  )(implicit tu: TransUserId, ec: ExecutionContext): Future[Option[A]] =
    getLatest(fid)(uid, tu, ec).flatMap {
      case Some(file) =>
        f(file.metadata.lock.map(_.by), file.id.get).map(Option.apply)

      case None => Future.successful(None)
    }

  /**
   * Places a lock on a file to prevent any modifications or new versions of
   * the file.
   *
   * @param uid UserId The id of the user that places the lock
   * @param fid FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will
   *         contain the applied lock.
   */
  def lock(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]]

  /**
   * Unlocks the provided file if and only if the provided user is the one
   * holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlock(fid: FileId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]]
}

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
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]]

  /**
   * Get the folder with the given FolderId.
   *
   * @param folderId FolderId
   * @return An Option with the found Folder.
   */
  def get(folderId: FolderId)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Option[Folder]]

  /**
   * Checks for the existence of a Folder
   *
   * @param f Folder
   * @return true if the folder exists, else false
   */
  def exists(f: Folder)(
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Boolean] = exists(f.flattenPath)

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param at Path to look for
   * @return true if the folder exists, else false
   */
  def exists(at: Path)(
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[CommandStatus[Int]]
}

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
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
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
      implicit uid: UserId,
      trans: TransUserId,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]]
}
