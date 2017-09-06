package net.scalytica.symbiotic.api.repository

import java.util.UUID

import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
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
   * @return An Option with the resulting ManagedFile.
   */
  def findLatestBy(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[A]]

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
    findLatestBy(fid).map(
      _.flatMap(fw => fw.metadata.lock.map(l => l.by))
    )
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

  /* Helper functions for lock related operations */

  protected def lockManagedFile(
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

  protected def unlockManagedFile(
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

  protected def lockedAnd[T](uid: UserId, fid: FileId)(
      f: (Option[UserId], UUID) => Future[T]
  )(implicit ctx: SymbioticContext, ec: ExecutionContext): Future[Option[T]] = {
    findLatestBy(fid).flatMap {
      case Some(file) =>
        f(file.metadata.lock.map(_.by), file.id.get).map(Option.apply)

      case None =>
        Future.successful(None)
    }
  }

}
