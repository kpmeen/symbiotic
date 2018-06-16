package net.scalytica.symbiotic.api.repository

import java.util.UUID

import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait ManagedFileRepo[A <: ManagedFile] {

  /**
   * Get the latest version of the ManagedFile with the given FileId.
   *
   * @param fid FileId
   * @return A GetResult with the resulting ManagedFile.
   */
  def findLatestBy(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[A]]

  /**
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return a GetResult with an Option[UserId] of the user holding the lock.
   */
  def locked(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Option[UserId]]] = {
    findLatestBy(fid).map(_.flatMap(fw => Ok(fw.metadata.lock.map(l => l.by))))
  }

  /**
   * Places a lock on a file to prevent any modifications or new versions of
   * the file.
   *
   * @param fid FileId of the file to lock
   * @return LockResult[Lock] with the applied lock.
   */
  def lock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockResult[Lock]]

  /**
   * Unlocks the provided file if and only if the provided user is the one
   * holding the current lock.
   *
   * @param fid FileId
   * @return UnlockResult[Unit]
   */
  def unlock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[UnlockResult[Unit]]

  /**
   * Method for checking if any parent folders in the given path are locked.
   *
   * @param from Path location to check
   * @return true if a parent is not locked, else false
   */
  def editable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean]

  /**
   * Method that sets the {{{isDeleted}}} flag in a
   * {{{ManagedFile#ManagedMetadata}}} to true. This will ensure that the file
   * is no longer returned by API calls for listing files in the tree.
   *
   * @param fid the FileId to mark as deleted
   * @return a DeleteResult[Int] with the number of successfully marked
   *         ManagedFiles. Typically 1:1 with number of versions of the resource
   */
  def markAsDeleted(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Int]]

  /* Helper functions for lock related operations */

  protected def lockManagedFile(
      fid: FileId
  )(f: (UUID, Lock) => Future[LockResult[Lock]])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockResult[Lock]] = lockedAnd(fid) {
    case (maybeUid, dbId) =>
      maybeUid.map { lockedBy =>
        Future.successful {
          ResourceLocked(
            msg = "Managed file is locked by another user.",
            by = lockedBy
          )
        }
      }.getOrElse {
        f(dbId, Lock(ctx.currentUser, DateTime.now())).recover {
          case NonFatal(e) =>
            Failed(s"Error trying to lock $fid: ${e.getMessage}")
        }
      }
  }

  protected def unlockManagedFile(
      fid: FileId
  )(f: UUID => Future[UnlockResult[Unit]])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[UnlockResult[Unit]] = lockedAnd(fid) {
    case (maybeUid, dbId) =>
      maybeUid.fold[Future[UnlockResult[Unit]]](
        Future.successful(NotLocked())
      ) {
        case usrId: UserId if ctx.currentUser.value == usrId.value =>
          f(dbId).recover {
            case NonFatal(e) =>
              Failed(s"Error trying to unlock $fid: ${e.getMessage}")
          }
        case usrId =>
          Future.successful {
            ResourceLocked(
              msg = "Managed file is locked by another user.",
              by = usrId
            )
          }
      }
  }

  protected def lockedAnd[T](fid: FileId)(
      f: (Option[UserId], UUID) => Future[SymRes[T]]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext): Future[SymRes[T]] = {
    findLatestBy(fid).flatMap {
      case Ok(file) => f(file.metadata.lock.map(_.by), file.id.get)
      case fail: Ko => Future.successful(fail)
    }
  }

}
