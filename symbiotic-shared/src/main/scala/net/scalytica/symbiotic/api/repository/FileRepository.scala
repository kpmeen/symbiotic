package net.scalytica.symbiotic.api.repository

import java.util.UUID

import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait FileRepository {

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param f File
   * @return Option[FileId]
   */
  def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]]

  /**
   * Get the latest version of the File with the given FileId.
   *
   * @param fid FolderId
   * @return An Option with the found File.
   */
  def findLatestByFileId(fid: FileId)(
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
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return an Option with the UserId of the user holding the lock
   */
  def locked(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[UserId]] = {
    findLatestByFileId(fid).map(
      _.flatMap(fw => fw.metadata.lock.map(l => l.by))
    )
  }

  protected def lockFile(
      fid: FileId
  )(f: (UUID, Lock) => Future[LockOpStatus[_ <: Option[Lock]]])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] =
    lockedAnd(ctx.currentUser, fid) {
      case (maybeUid, dbId) =>
        maybeUid
          .map(lockedBy => Future.successful(Locked(lockedBy)))
          .getOrElse {
            f(dbId, Lock(ctx.currentUser, DateTime.now())).recover {
              case NonFatal(e) =>
                LockError(s"Error trying to lock $fid: ${e.getMessage}")
            }
          }
    }.map(_.getOrElse(LockError(s"File $fid was not found")))

  protected def unlockFile(
      fid: FileId
  )(f: UUID => Future[LockOpStatus[_ <: String]])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] =
    lockedAnd(ctx.currentUser, fid) {
      case (maybeUid, dbId) =>
        maybeUid.fold[Future[LockOpStatus[_ <: String]]](
          Future.successful(NotLocked())
        ) {
          case usrId: UserId if ctx.currentUser.value == usrId.value =>
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
  )(implicit ctx: SymbioticContext, ec: ExecutionContext): Future[Option[A]] =
    findLatestByFileId(fid).flatMap {
      case Some(file) =>
        f(file.metadata.lock.map(_.by), file.id.get).map(Option.apply)

      case None => Future.successful(None)
    }

  /**
   * Places a lock on a file to prevent any modifications or new versions of
   * the file.
   *
   * @param fid FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will
   *         contain the applied lock.
   */
  def lock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]]

  /**
   * Unlocks the provided file if and only if the provided user is the one
   * holding the current lock.
   *
   * @param fid FileId
   * @return
   */
  def unlock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]]
}
