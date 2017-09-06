package net.scalytica.symbiotic.core

import net.scalytica.symbiotic.api.repository.ManagedFileRepo
import net.scalytica.symbiotic.api.types.CommandStatusTypes._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes.{
  LockApplied,
  LockError,
  LockRemoved
}
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{ManagedFile, Path, _}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off number.of.methods

/**
 * Singleton object that provides document management operations towards the
 * repository. Operations allow access to both Folders and Files, including the
 * complete stream that provides a handle to the physical instance.
 */
final class DocManagementService(
    resolver: ConfigResolver = new ConfigResolver()
) {

  private val folderRepository = resolver.repoInstance.folderRepository
  private val fileRepository   = resolver.repoInstance.fileRepository
  private val fstreeRepository = resolver.repoInstance.fsTreeRepository

  private val log = LoggerFactory.getLogger(this.getClass)

  /**
   * Helper function to ensure that a check for any locks in the tree where
   * the modification of the ManagedFile is applied. If there is such a folder
   * lock, the operation will abort with the fail function {{{f}}}. Otherwise
   * the success function {{{m}}} is executed.
   */
  private[this] def modifyManagedFile[A](p: Path)(m: => Future[A])(f: => A)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[A] = canBeEdited(p).flatMap(e => if (e) m else Future.successful(f))

  /**
   * Helper function to ensure that a check for any locks in both the original
   * and destination trees involved in the move operation. If there is such a
   * folder lock, the operation will abort with the fail function {{{f}}}.
   * Otherwise the success function {{{m}}} is executed.
   */
  private[this] def moveManagedFile[A](
      o: Path,
      d: Path
  )(mv: => Future[A])(f: => A)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[A] = {
    val checkOrig = canBeEdited(o)
    val checkDest = canBeEdited(d)

    for {
      oe  <- checkOrig
      de  <- checkDest
      res <- if (oe && de) mv else Future.successful(f)
    } yield res
  }

  /**
   * Function allowing renaming of folder segments!
   * - Find the tree of folders from the given path element to rename
   * - Update all path segments with the new name for the given path element.
   * - Return all folders that were affected
   *
   * @param orig Path with the original full path
   * @param mod  Path with the modified full path
   * @return A collection containing the folder paths that were updated.
   */
  def moveFolder(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[Path]] = {
    moveManagedFile(orig, mod) {
      for {
        fps <- treeWithFiles(Some(orig)).map(_.flatMap(_.metadata.path))
        upd <- Future.successful {
                fps.map { fp =>
                  fp -> Path(fp.value.replaceAll(orig.value, mod.value))
                }
              }
        res <- Future.sequence {
                upd.map {
                  case (o, d) =>
                    folderRepository.move(o, d).map(cs => (o, d) -> cs)
                }
              }
      } yield {
        res.map {
          case ((fp, up), cs) =>
            cs match {
              case CommandOk(_) =>
                Option(up)

              case CommandKo(_) =>
                log.warn(s"Path ${fp.value} was not updated to ${up.value}")
                None

              case CommandError(_, m) =>
                log.error(
                  s"An error occurred when trying to update path" +
                    s" ${fp.value} to ${up.value}. Message is: $m"
                )
                None
            }
        }.filter(_.isDefined).flatten
      }
    } {
      log.warn(
        s"Can't move folder $orig because it or its destination is in a" +
          s" locked sub-tree"
      )
      Seq.empty
    }
  }

  /**
   * This method will return the a collection of files, representing the
   * folder/directory structure that has been set-up in the repository.
   *
   * @param from Path location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of BaseFile instances that match the criteria
   */
  def treeWithFiles(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = fstreeRepository.tree(from)

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param from Path location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treeNoFiles(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[Folder]] =
    fstreeRepository.tree(from).map(_.flatMap(Folder.mapTo))

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param from Folder location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of Paths that match the criteria.
   */
  def treePaths(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[(FileId, Path)]] =
    fstreeRepository.treePaths(from).map { paths =>
      log.debug(s"Found paths ${paths.mkString("\n", "\n", "\n")}")
      paths
    }

  /**
   * This method will return a collection of File instances , representing the
   * direct descendants for the given Folder.
   *
   * @param from Path location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of BaseFile instances that match the criteria
   */
  def childrenWithFiles(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[ManagedFile]] = fstreeRepository.children(from)

  /**
   * Moves a file to another folder if, and only if, the folder doesn't contain
   * a file with the same name, the current or destination paths are editable.
   *
   * @param filename String
   * @param orig     Path
   * @param mod      Path the folder to place the file
   * @return An Option with the updated File
   */
  def moveFile(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] =
    moveManagedFile(orig, mod) {
      fileRepository.findLatest(filename, Some(mod)).flatMap { maybeLatest =>
        maybeLatest.fold(fileRepository.move(filename, orig, mod)) { _ =>
          log.info(
            s"Not moving file $filename to $mod because a file with " +
              s"the same name already exists."
          )
          Future.successful(None)
        }
      }
    } {
      log.warn(
        s"Can't move file $filename because it or its destination is in a" +
          s" locked sub-tree"
      )
      None
    }

  /**
   * Moves a file with a specific FileId from the given original path, to a new
   * destination path. The move takes place iff the destination doesn't contain
   * a file with the same name.
   *
   * @param fileId FileID
   * @param orig   Path
   * @param mod    Path
   * @return Returns an Option with the updated file.
   */
  def moveFile(fileId: FileId, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] =
    moveManagedFile(orig, mod) {
      fileRepository.findLatestBy(fileId).flatMap { maybeLatest =>
        maybeLatest.map(fw => moveFile(fw.filename, orig, mod)).getOrElse {
          log.info(s"Could not find file with with id $fileId")
          Future.successful(None)
        }
      }
    } {
      log.warn(
        s"Can't move file $fileId because it or its destination is in a" +
          s" locked sub-tree"
      )
      None
    }

  /**
   * Get the folder with the given FolderId.
   *
   * @param folderId FolderId
   * @return An Option with the found Folder.
   */
  def folder(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = folderRepository.get(folderId)

  /**
   * Get the folder at the given Path.
   *
   * @param at Path to look for
   * @return An Option with the found Folder.
   */
  def folder(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = folderRepository.get(at)

  /**
   * Attempt to create a folder. If successful it will return the FolderId.
   * If segments of the Folder path is non-existing, these will be created as
   * well.
   *
   * @param at Path to create
   * @return maybe a FileId if it was successfully created
   */
  def createFolder(at: Path, createMissing: Boolean = true)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FolderId]] =
    modifyManagedFile(at) {
      if (createMissing) {
        log.debug(s"Create folder $at, and any missing parent folders")
        for {
          fid   <- folderRepository.save(Folder(ctx.owner.id, at))
          paths <- createNonExistingFoldersInPath(at)
        } yield {
          if (paths.nonEmpty) {
            log.debug(s"Created folders for ${paths.mkString(" - ")}")
          } else {
            log.debug(s"No additional paths were created.")
          }
          fid
        }
      } else {
        saveFolder(Folder(ctx.owner.id, at))
      }
    } {
      log.warn(s"Can't create folder because the sub-tree $at is locked")
      None
    }

  /**
   * Attempt to create a folder at the given path with a specified folder type
   * and extra metadata attributes.
   *
   * @param at              the Path to create the Folder at
   * @param folderType      Optional String describing the type of folder
   * @param extraAttributes Optional MetadataMap with extra metadata values
   * @return maybe a FileId if it was successfully created.
   */
  def createFolder(
      at: Path,
      folderType: Option[String],
      extraAttributes: Option[MetadataMap]
  )(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FolderId]] =
    modifyManagedFile(at) {
      saveFolder(Folder(ctx.owner.id, at, folderType, extraAttributes))
    } {
      log.warn(s"Can't create folder because the sub-tree $at is locked")
      None
    }

  /**
   * Attempts to create the provided folder in the repository. In this case it
   * is up to the consumer of the method to ensure that all required fields are
   * correctly populated etc. In particular it is important to ensure the path
   * and name of the folder is correct.
   *
   * @param f the Folder to persist
   * @return maybe a FileId if it was successfully created.
   */
  def createFolder(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FolderId]] = {
    f.metadata.path.map { at =>
      modifyManagedFile(at)(saveFolder(f)) {
        log.warn(s"Can't create folder because the sub-tree $at is locked.")
        None
      }
    }.getOrElse {
      log.warn(s"Can't create folder because the path is not specified.")
      Future.successful(None)
    }
  }

  /**
   * Attempt to store the provided [[Folder]] in the repository.
   *
   * @param f the Folder to create
   * @return maybe a FileId if it was successfully created.
   */
  private[this] def saveFolder(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FolderId]] = {
    val verifyPath = f.flattenPath.materialize
      .split(",")
      .filterNot(_.isEmpty)
      .dropRight(1)
      .mkString("/", "/", "/")

    val vf = Path(verifyPath)
    folderRepository.filterMissing(vf).flatMap { missing =>
      if (missing.isEmpty) {
        log.debug(s"Parent folders exist, creating folder $verifyPath")
        folderRepository.save(f)
      } else {
        log.warn(
          s"Did not create folder because there are missing parent" +
            s" folders for $verifyPath."
        )
        Future.successful(None)
      }
    }
  }

  /**
   * Will create any missing path segments found in the Folder path, and return
   * a List of all the Folders that were created.
   *
   * @param p Path to verify path and create non-existing segments
   * @return A List containing the missing folders that were created.
   */
  private[this] def createNonExistingFoldersInPath(p: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[List[Path]] = {
    for {
      missing <- folderRepository.filterMissing(p)
      inserted <- Future.sequence {
                   missing.map { mp =>
                     folderRepository.save(Folder(ctx.owner.id, mp))
                   }
                 }
    } yield {
      log.debug(s"Missing folders were ${missing.mkString(" - ")}")
      log.debug(s"Created folders ${inserted.mkString(", ")}")
      missing
    }
  }

  /**
   * Convenience function for creating the root Folder.
   *
   * @return maybe a FileId if the root folder was created
   */
  def createRootFolder(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] =
    folderRepository.save(Folder.root(ctx.owner.id))

  /**
   * Service for updating metadata for an existing Folder.
   *
   * @param f the Folder to update
   * @return
   */
  def updateFolder(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] =
    modifyManagedFile(f.flattenPath) {
      folderRepository.save(f)
    } {
      log.warn(
        s"Can't update folder because the sub-tree ${f.flattenPath} is locked"
      )
      None
    }

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param at Path with the path to look for
   * @return true if the folder exists, else false
   */
  def folderExists(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = folderRepository.exists(at)

  /**
   * Helper function to initialize a Root folder if one doesn't exist for the
   * implicitly given UserId of the current user.
   */
  private[this] def createRootIfNotExists(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    folderRepository
      .exists(Path.root)
      .flatMap(e => if (!e) createRootFolder else Future.successful(None))
  }

  private[this] def saveOpt(f: File, soml: Either[String, Option[File]])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    soml match {
      case Right(maybeLatest) =>
        maybeLatest.map { latest =>
          val canSave = latest.metadata.lock.fold(false) { l =>
            l.by == ctx.currentUser
          }

          if (canSave) {
            fileRepository.save(
              f.copy(
                metadata = f.metadata.copy(
                  fid = latest.metadata.fid,
                  version = latest.metadata.version + 1,
                  lock = latest.metadata.lock
                )
              )
            )
          } else {
            if (latest.metadata.lock.isDefined)
              log.warn(
                s"Cannot save file because it is locked by another " +
                  s"user: ${latest.metadata.lock.map(_.by).getOrElse("<NA>")}"
              )
            else
              log.warn(
                s"Cannot save file because the file isn't locked."
              )
            Future.successful(None)
          }
        }.getOrElse {
          log.debug(
            s"This is the first time the file ${f.filename} is uploaded."
          )
          fileRepository.save(
            f.copy(metadata = f.metadata.copy(fid = FileId.createOpt()))
          )
        }

      case Left(errString) =>
        log.debug(
          s"Can't save argument because it contained an error: $errString"
        )
        Future.successful(None)
    }

  }

  /**
   * Saves the passed on File in the file repository.
   *
   * @param f File
   * @return Option[FileId]
   */
  def saveFile(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = {
    val dest = f.metadata.path.getOrElse(Path.root)

    modifyManagedFile(dest) {
      for {
        _          <- createRootIfNotExists
        destExists <- folderRepository.exists(dest)
        stringOrMaybeLatest <- {
          log.debug(s"Destination exists = $destExists")
          if (destExists) {
            fileRepository
              .findLatest(f.filename, f.metadata.path)
              .map(Right.apply)
          } else {
            log.warn(
              s"Attempted to save file to non-existing destination " +
                s"folder: ${dest.value}, materialized as ${dest.materialize}"
            )
            Future.successful(Left("Not saveable"))
          }
        }
        _ <- stringOrMaybeLatest.right.toOption.flatten
              .map(latest => unlockFile(latest.metadata.fid.get))
              .getOrElse(Future.successful(true))
        maybeSavedId <- saveOpt(f, stringOrMaybeLatest)
      } yield {
        maybeSavedId.foreach { sid =>
          log.debug(s"Saved file $sid to ${f.metadata.path}")
        }
        maybeSavedId
      }
    } {
      log.warn(s"Can't save file because the folder tree $dest is locked")
      None
    }
  }

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[File]
   */
  def file(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = fileRepository.findLatestBy(fid)

  /**
   * Will return a collection of File (if found) with the provided filename and
   * folder properties.
   *
   * @param filename  String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def listFiles(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]] = fileRepository.find(filename, maybePath)

  /**
   * Will return the latest version of a file (File)
   *
   * @param filename  String
   * @param maybePath Option[Path]
   * @return An Option with a File
   */
  def latestFile(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = fileRepository.findLatest(filename, maybePath)

  /**
   * List all the files in the given Folder path for the given OrgId
   *
   * @param path Path
   * @return Option[File]
   */
  def listFiles(path: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]] = fileRepository.listFiles(path)

  /**
   * Places a lock on a file to prevent any modifications or new versions from
   * anyone else than the user placing the lock.
   *
   * @param fileId FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will
   *         contain the applied lock.
   */
  def lockFile(fileId: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Lock]] =
    file(fileId).flatMap {
      _.flatMap(_.metadata.path).map { path =>
        modifyManagedFile(path) {
          fileRepository.lock(fileId).map {
            case LockApplied(s) =>
              log.info(s"Lock applied for file $fileId"); s
            case LockError(r) => log.warn(r); None
            case _            => None
          }
        } {
          log.warn(
            s"Can't lock file $fileId because it's in a locked sub-tree"
          )
          None
        }
      }.getOrElse(Future.successful(None))
    }

  /**
   * Places a lock on a folder to prevent any modifications or any new versions.
   *
   * Where as a file lock prevents mutations or new versions of a single file,
   * the folder lock applies to the specified folder _and_ the entire sub-tree.
   * This behaviour can be used to create immutable archives of an entire
   * folder tree.
   *
   * @param folderId FolderId of the folder to lock
   * @return Option[Lock] None if no lock was applied, else the Option will
   *         contain the applied lock.
   */
  def lockFolder(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Lock]] =
    folder(folderId).flatMap {
      _.map { folder =>
        modifyManagedFile(folder.flattenPath) {
          folderRepository.lock(folderId).map {
            case LockApplied(s) => log.info(s"Lock applied folder $folderId"); s
            case LockError(r)   => log.warn(r); None
            case _              => None
          }
        } {
          log.warn(
            s"Can't lock file $folderId because it's in a locked sub-tree"
          )
          None
        }
      }.getOrElse(Future.successful(None))
    }

  /**
   * Unlocks the provided file if and only if the current user is the one
   * holding the lock.
   *
   * @param fileId FileId
   * @return true if the lock was removed, else false
   */
  def unlockFile(fileId: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] =
    file(fileId).flatMap {
      _.flatMap(_.metadata.path).map { path =>
        modifyManagedFile(path) {
          fileRepository.unlock(fileId).map {
            case LockRemoved(_) => log.info(s"Unlocked file $fileId"); true
            case LockError(r)   => log.warn(r); false
            case _              => false
          }
        } {
          log.warn(
            s"Can't unlock file $fileId because it's in a locked sub-tree"
          )
          false
        }
      }.getOrElse(Future.successful(false))
    }

  /**
   * Unlocks the provided folder if and only if the current user is the one
   * holding the lock.
   *
   * @param folderId FolderId
   * @return true if the lock was removed, else false
   */
  def unlockFolder(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] =
    folderRepository.unlock(folderId).map {
      case LockRemoved(_) => log.info(s"Unlocked folder $folderId"); true
      case LockError(r)   => log.warn(r); false
      case _              => false
    }

  /**
   * Checks if the file has a lock or not
   *
   * @param fileId FileId
   * @return true if locked, else false
   */
  def fileHasLock(fileId: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = hasLock(fileId)(fileRepository)

  /**
   * Checks if the folder has a lock or not
   *
   * @param folderId FolderId
   * @return true if locked, else false
   */
  def folderHasLock(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = hasLock(folderId)(folderRepository)

  /**
   * Helper method to check for lock on managed file in given repository
   */
  private def hasLock[A <: ManagedFile](fid: FileId)(repo: ManagedFileRepo[A])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = repo.locked(fid).map(_.isDefined)

  /**
   * Checks if the file is locked and if it is locked by the given user
   *
   * @param fileId FileId
   * @param uid    UserId
   * @return true if locked by user, else false
   */
  def fileIsLockedBy(fileId: FileId, uid: UserId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = isLockedBy(fileId, uid)(fileRepository)

  /**
   * Checks if the file is locked and if it is locked by the given user
   *
   * @param folderId FolderId
   * @param uid UserId
   * @return true if locked by user, else false
   */
  def folderIsLockedBy(folderId: FolderId, uid: UserId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = isLockedBy(folderId, uid)(folderRepository)

  /**
   * Helper method to execute check for lock owned by user in the provided
   * repository.
   */
  private def isLockedBy[A <: ManagedFile](
      fid: FileId,
      uid: UserId
  )(repo: ManagedFileRepo[A])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = repo.locked(fid).map(_.contains(uid))

  /**
   * Check if a ManagedFile can be edited/modified in any way. To be editable
   * the ManagedFile must not be in a folder or sub-tree that is locked.
   *
   * @param from Path to check if can be edited
   * @return returns true if it can be modified, else false
   */
  private[core] def canBeEdited(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = folderRepository.editable(from)
}
// scalastyle:on number.of.methods
