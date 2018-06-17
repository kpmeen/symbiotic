package net.scalytica.symbiotic.core

import net.scalytica.symbiotic.api.repository.ManagedFileRepo
import net.scalytica.symbiotic.api.SymbioticResults.{MoveResult, _}
import net.scalytica.symbiotic.api.functional.MonadTransformers.SymResT
import net.scalytica.symbiotic.api.functional.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{ManagedFile, Path, _}
import net.scalytica.symbiotic.config.ConfigResolver
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off number.of.methods file.size.limit

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

  log.warn(s"Initializing new instance of $getClass")

  /**
   * Helper function to ensure that a check for any locks in the tree where
   * the modification of the ManagedFile is applied. If there is such a folder
   * lock, the operation will abort with the fail function {{{f}}}. Otherwise
   * the success function {{{m}}} is executed.
   */
  private[this] def modifyManagedFile[A](p: Path)(m: => Future[A])(f: => A)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[A] = {
    canBeEdited(p).flatMap(e => if (e) m else Future.successful(f))
  }

  /**
   * Helper function to ensure that a check for any locks in both the original
   * and destination trees involved in the move operation. If there is such a
   * folder lock, the operation will abort with the fail function {{{bad}}}.
   * Otherwise the success function {{{mv}}} is executed.
   */
  private[this] def moveManagedFile[A](
      o: Path,
      d: Path
  )(mv: => Future[A])(bad: => A)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[A] = {
    val checkOrig = canBeEdited(o)
    val checkDest = canBeEdited(d)

    for {
      oe  <- checkOrig
      de  <- checkDest
      res <- if (oe && de) mv else Future.successful(bad)
    } yield res
  }

  /**
   * Function for moving a folder and its children! The basic functionality is
   * much like the linux mv command, where it can also be used to rename a given
   * folder. If successful it will return a list of all the paths that were
   * affected by the move.
   *
   * @param orig Path with the original full path
   * @param mod  Path with the modified full path
   * @return A collection containing the folder paths that were updated.
   */
  def moveFolder(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[MoveResult[Seq[Path]]] = {
    def movedPaths(res: MoveResult[Int], p: Path): Future[Seq[Path]] =
      res.toOption.flatMap { n =>
        if (n > 0) {
          Some(treeWithFiles(Some(p)).map { r =>
            r.map { files =>
              log.debug(s"Found ${files.map(_.flattenPath).mkString("\n")}")
              files.map(_.flattenPath).distinct
            }.getOrElse(Seq.empty)
          })
        } else None
      }.getOrElse(Future.successful(Seq.empty))

    moveManagedFile(orig, mod) {
      for {
        res      <- folderRepository.move(orig, mod)
        affected <- movedPaths(res, mod)
      } yield {
        res match {
          case Ok(_) =>
            Ok(affected)

          case NotModified() =>
            log.warn(s"Paths starting with $orig was not updated to $mod")
            NotModified()

          case ko: Ko =>
            log.error(
              s"An error occurred when trying to update path" +
                s" ${orig.value} to ${mod.value}. Reason is: $ko"
            )
            ko
        }
      }
    } {
      log.warn(
        s"Can't move folder $orig because it or its destination is in a" +
          s" locked sub-tree"
      )
      NotModified()
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
  ): Future[GetResult[Seq[ManagedFile]]] = fstreeRepository.tree(from)

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
  ): Future[GetResult[Seq[Folder]]] =
    fstreeRepository.tree(from).map(_.map(_.flatMap(Folder.mapTo)))

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
  ): Future[GetResult[Seq[(FileId, Path)]]] =
    fstreeRepository.treePaths(from).map { res =>
      res.foreach(p => log.debug(s"Found res ${p.mkString("\n", "\n", "\n")}"))
      res
    }

  /**
   * This method will return a collection of File instances , representing the
   * direct descendants for the given Folder.
   *
   * @param from Path location to return the tree structure from. Defaults
   *             to rootFolder
   * @return a collection of ManagedFile instances that match the criteria
   */
  def childrenWithFiles(from: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[ManagedFile]]] = fstreeRepository.children(from)

  /**
   * Moves a file to another folder if, and only if, the folder doesn't contain
   * a file with the same name, the current or destination paths are editable.
   *
   * @param filename String
   * @param orig     Path
   * @param mod      Path the folder to place the file
   * @return The updated File
   */
  def moveFile(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[MoveResult[File]] =
    moveManagedFile(orig, mod) {
      fileRepository.findLatest(filename, Some(mod)).flatMap {
        case Ok(_) =>
          val msg = s"Not moving file $filename to $mod because a file with " +
            s"the same name already exists."
          log.info(msg)
          Future.successful(IllegalDestination(msg, mod))

        case _ =>
          fileRepository.move(filename, orig, mod)
      }
    } {
      val msg = s"Can't move file $filename because it or its destination is " +
        "in a locked sub-tree"
      log.warn(msg)
      ResourceLocked(msg)
    }

  /**
   * Moves a file with a specific FileId from the given original path, to a new
   * destination path. The move takes place iff the destination doesn't contain
   * a file with the same name.
   *
   * @param fileId FileID
   * @param orig   Path
   * @param mod    Path
   * @return Returns the updated file.
   */
  def moveFile(fileId: FileId, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[MoveResult[File]] =
    moveManagedFile(orig, mod) {
      fileRepository.findLatestBy(fileId).flatMap {
        case Ok(fw) => moveFile(fw.filename, orig, mod)
        case ko     => Future.successful(ko)
      }
    } {
      val msg = s"Can't move file $fileId because it or its destination is " +
        "in a locked sub-tree"
      log.warn(msg)
      ResourceLocked(msg)
    }

  /**
   * Get the folder with the given FolderId.
   *
   * @param folderId FolderId
   * @return The found Folder.
   */
  def folder(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] = folderRepository.get(folderId)

  /**
   * Get the folder at the given Path.
   *
   * @param at Path to look for
   * @return The found Folder.
   */
  def folder(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] = folderRepository.get(at)

  /**
   * Attempt to create a folder. If successful it will return the FolderId.
   * If segments of the Folder path is non-existing, these will be created as
   * well.
   *
   * @param at Path to create
   * @return The FileId of the created Folder
   */
  def createFolder(at: Path, createMissing: Boolean = true)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FolderId]] =
    modifyManagedFile(at) {
      if (createMissing) {
        log.debug(s"Create folder $at, and any missing parent folders")
        for {
          fid   <- SymResT(folderRepository.save(Folder(ctx.owner.id, at)))
          paths <- SymResT(createNonExistingFoldersInPath(at))
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
      val msg = s"Can't create folder because the sub-tree $at is locked"
      log.warn(msg)
      ResourceLocked(msg)
    }

  /**
   * Attempt to create a folder at the given path with a specified folder type
   * and extra metadata attributes.
   *
   * @param at              the Path to create the Folder at
   * @param folderType      Optional String describing the type of folder
   * @param extraAttributes Optional MetadataMap with extra metadata values
   * @return The FileId of the newly created Folder
   */
  def createFolder(
      at: Path,
      folderType: Option[String],
      extraAttributes: Option[MetadataMap]
  )(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FolderId]] =
    modifyManagedFile(at) {
      saveFolder(Folder(ctx.owner.id, at, folderType, extraAttributes))
    } {
      val msg = s"Can't create folder because the sub-tree $at is locked"
      log.warn(msg)
      ResourceLocked(msg)
    }

  /**
   * Attempts to create the provided folder in the repository. In this case it
   * is up to the consumer of the method to ensure that all required fields are
   * correctly populated etc. In particular it is important to ensure the path
   * and name of the folder is correct.
   *
   * @param f the Folder to persist
   * @return The FileId of the newly created Folder
   */
  def createFolder(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FolderId]] = {
    f.metadata.path.map { at =>
      modifyManagedFile(at)(saveFolder(f)) {
        val msg = s"Can't create folder because the sub-tree $at is locked"
        log.warn(msg)
        ResourceLocked(msg)
      }
    }.getOrElse {
      val msg = s"Can't create folder because the path is not specified."
      log.warn(msg)
      Future.successful(InvalidData(msg))
    }
  }

  /**
   * Attempt to store the provided [[Folder]] in the repository.
   *
   * @param f the Folder to create
   * @return The FileId of the newly created Folder
   */
  private[this] def saveFolder(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FolderId]] = {
    val verifyPath = f.flattenPath.materialize
      .split(",")
      .filterNot(_.isEmpty)
      .dropRight(1)
      .mkString("/", "/", "/")

    val vf = Path(verifyPath)
    folderRepository.filterMissing(vf).flatMap {
      case Ok(missing) =>
        if (missing.isEmpty) {
          log.debug(s"Parent folders exist, creating folder $verifyPath")
          folderRepository.save(f)
        } else {
          val msg = s"Did not create folder because there are missing parent" +
            s" folders for $verifyPath."
          log.warn(msg)
          Future.successful(InvalidData(msg))
        }

      case ko: Ko => Future.successful(ko)
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
  ): Future[SaveResult[List[Path]]] = {

    for {
      missing <- SymResT(folderRepository.filterMissing(p))
      inserted <- SymResT.sequenceF {
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
   * @return The FileId if the root folder was created
   */
  def createRootFolder(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FileId]] =
    folderRepository.save(Folder.root(ctx.owner.id))

  /**
   * Service for updating metadata for an existing Folder.
   *
   * @param f the Folder to update
   * @return The FolderId of the updated folder
   */
  def updateFolder(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FileId]] =
    modifyManagedFile(f.flattenPath) {
      folderRepository.save(f)
    } {
      val msg = s"Can't update folder in locked sub-tree ${f.flattenPath}"
      log.warn(msg)
      ResourceLocked(msg)
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
  def createRootIfNotExists(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FileId]] = {
    folderRepository.exists(Path.root).flatMap { e =>
      if (!e) createRootFolder
      else Future.successful(InvalidData("Root already exists"))
    }
  }

  private[this] def preSave[A](
      f: File,
      latestRes: GetResult[File]
  )(
      saveFirst: () => Future[SaveResult[A]]
  )(
      saveVersion: (File, File) => Future[SaveResult[A]]
  )(
      implicit ctx: SymbioticContext
  ): Future[SaveResult[A]] = {
    latestRes match {
      case Ok(latest) =>
        val maybeLock = latest.metadata.lock
        val canSave   = maybeLock.fold(false)(_.by == ctx.currentUser)

        if (canSave) {
          saveVersion(f, latest)
        } else {
          if (maybeLock.isDefined) {
            log.warn(
              s"Cannot save file because it is locked by another " +
                s"user: ${latest.metadata.lock.map(_.by).getOrElse("<NA>")}"
            )
            Future.successful {
              ResourceLocked(
                "File is locked by another user",
                latest.metadata.lock.map(_.by)
              )
            }
          } else {
            log.warn(
              s"Cannot save file because the file isn't locked."
            )
            Future.successful(NotLocked())
          }
        }

      case NotFound() =>
        saveFirst()

      case ko: Ko =>
        Future.successful(ko)
    }
  }

  private[this] def updateOpt(f: File, latest: GetResult[File])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[File]] = {
    preSave[File](f, latest) { () =>
      log.debug(s"File ${f.filename} at ${f.metadata.path} could not be found")
      Future.successful(NotFound())
    } { (newFile, latestFile) =>
      val toUpdate = latestFile.copy(
        metadata = latestFile.metadata.copy(
          description = newFile.metadata.description,
          extraAttributes = newFile.metadata.extraAttributes
        )
      )

      fileRepository.updateMetadata(toUpdate)
    }
  }

  private[this] def saveOpt(f: File, latest: GetResult[File])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FileId]] = {
    preSave[FileId](f, latest) { () =>
      log.debug(s"This is the first time the file ${f.filename} is uploaded.")

      val md = f.metadata
        .grantAccess(ctx.currentUser)
        .grantAccess(ctx.owner.id)
        .copy(fid = FileId.createOpt())

      fileRepository.save(f.copy(metadata = md))
    } { (newFile, latestFile) =>
      val toSave = newFile.copy(
        metadata = newFile.metadata.copy(
          fid = latestFile.metadata.fid,
          version = latestFile.metadata.version + 1,
          lock = latestFile.metadata.lock
        )
      )
      fileRepository.save(toSave)
    }

  }

  private[this] def saveFileOp[A](dest: Path, f: File)(
      persistFile: (File, GetResult[File]) => Future[SaveResult[A]]
  )(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[A]] = {
    for {
      _          <- createRootIfNotExists
      destExists <- folderRepository.exists(dest)
      latest <- if (destExists) {
                 fileRepository.findLatest(f.filename, f.metadata.path)
               } else {
                 val msg =
                   s"Attempted to save file to non-existing destination folder"
                 Future.successful(IllegalDestination(msg, dest))
               }
      _ <- latest
            .map(f => unlockFile(f.metadata.fid.get))
            .getOrElse(Future.successful(true))
      savedIdRes <- persistFile(f, latest)
    } yield {
      savedIdRes.foreach { sid =>
        log.debug(s"Saved file $sid to ${f.metadata.path}")
      }
      savedIdRes
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
  ): Future[SaveResult[FileId]] = {
    val dest = f.metadata.path.getOrElse(Path.root)

    modifyManagedFile(dest)(saveFileOp(dest, f)(saveOpt)) {
      val msg = s"Can't save file because the folder tree $dest is locked"
      log.warn(msg)
      ResourceLocked(msg)
    }
  }

  /**
   * Similar to [[saveFile]], but with slightly different semantics around the
   * File's path.
   *
   * @param f File
   * @return Option[FileId]
   */
  def updateFile(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[File]] = {
    f.metadata.path.map { p =>
      modifyManagedFile(p)(saveFileOp(p, f)(updateOpt)) {
        val msg = s"Can't save file because the folder tree $p is locked"
        log.warn(msg)
        ResourceLocked(msg)
      }
    }.getOrElse {
      val msg = s"Can't update file because it has no path specified."
      log.warn(msg)
      Future.successful(InvalidData(msg))
    }
  }

  private[this] def canRemoveFromTree(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = {
    fstreeRepository.tree(f.metadata.path).map {
      case Ok(mfs) =>
        val hasLockedFolders = mfs.exists { mf =>
          mf.metadata.isFolder && mf.metadata.lock.isDefined
        }

        val hasFiles = mfs.exists { mfs =>
          !mfs.metadata.isFolder && !mfs.metadata.isDeleted
        }

        if (hasLockedFolders) {
          // We have locked folders in the sub-tree
          log.warn(
            s"Can't remove ${f.filename} on path ${f.flattenPath} because " +
              s"it contains locked folders in its sub-tree."
          )
        }
        if (hasFiles) {
          // We have non-deleted files in the sub-tree
          log.warn(
            s"Can't remove ${f.filename} on path ${f.flattenPath} because " +
              s"it contains files in its sub-tree."
          )
        }

        !hasFiles && !hasLockedFolders

      case ko =>
        log.warn(s"Can't verify tree since result from tree query was $ko")
        false
    }
  }

  /**
   * Will attempt to set the {{{isDeleted}}} flag in the metadata for the given
   * FolderId. The operation will only succeed if the Folder is _not_ locked,
   * regardless of _who_ has placed the Lock.
   *
   * @param folderId FolderId to mark as deleted
   * @return eventually an Either containing a Unit on the rhs in case of
   *         success, or a String on the lhs if there's an error.
   */
  def deleteFolder(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Unit]] = {
    folderRepository.findLatestBy(folderId).flatMap {
      case Ok(f) =>
        canRemoveFromTree(f).flatMap { canRemove =>
          if (canRemove) {
            if (f.metadata.lock.isEmpty) {
              f.metadata.path.map { p =>
                modifyManagedFile(p) {
                  folderRepository
                    .markAsDeleted(folderId)
                    .map(_.map { numMarked =>
                      log.debug(s"Deleted $numMarked versions of $folderId")
                    })
                } {
                  val msg =
                    s"Can't mark $folderId deleted because tree in $p is locked"
                  log.warn(msg)
                  ResourceLocked(msg)
                }
              }.getOrElse {
                Future.successful(
                  InvalidData(s"$folderId did not contain a valid path!")
                )
              }
            } else {
              Future.successful {
                ResourceLocked(
                  s"Folder $folderId is locked and can't be marked as deleted.",
                  f.metadata.lock.map(_.by)
                )
              }
            }
          } else {
            Future.successful {
              ResourceLocked(
                s"Can't delete $folderId because it contains locked folders " +
                  s"or files in its sub-tree."
              )
            }
          }
        }

      case ko: Ko =>
        Future.successful(ko)
    }
  }

  /**
   * Will attempt to set the {{{isDeleted}}} flag in the metadata for the given
   * FileId. The operation will not succeed if the File has a lock that is not
   * owned by the currentUser of the current context. When the file _is_ marked
   * as deleted, the previously held lock is also removed.
   *
   * @param fileId FileId to mark as deleted
   * @return eventually an Either containing a Unit on the rhs in case of
   *         success, or a String on the lhs if there's an error.
   */
  def deleteFile(fileId: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Unit]] = {
    removeFile(fileId) { file =>
      fileRepository
        .markAsDeleted(fileId)
        .map(_.map { num =>
          log.debug(s"Deleted $num versions of $fileId")
          file.metadata.lock.foreach { _ =>
            log.debug(s"Unlocking $fileId")
            fileRepository.unlock(fileId)
          }
        })
    }
  }

  /**
   * ¡¡¡DANGER!!! This method will remove a File and all its versions completely
   * from the file repository. Both physical files and their metadata. Think
   * twice before using this as its effect is irreversible.
   *
   * @param fileId the FileId of the file to completely remove
   * @return a Future containing a DeleteResult[Unit]
   */
  def eraseFile(fileId: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Unit]] = {
    removeFile(fileId) { _ =>
      fileRepository
        .eraseFile(fileId)
        .map(_.map(num => log.debug(s"Erased $num versions of $fileId")))
    }
  }

  private def removeFile(
      fileId: FileId
  )(
      remove: File => Future[DeleteResult[Unit]]
  )(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Unit]] = {
    fileRepository.findLatestBy(fileId).flatMap {
      case Ok(f) =>
        if (f.metadata.lock.forall(_.by == ctx.currentUser)) {
          f.metadata.path.map { p =>
            modifyManagedFile(p)(remove(f)) {
              val msg = s"Can't erase $fileId because tree in $p is locked"
              log.warn(msg)
              ResourceLocked(msg, f.metadata.lock.map(_.by))
            }
          }.getOrElse {
            Future
              .successful(InvalidData(s"$fileId did not contain a valid path!"))
          }
        } else {
          Future.successful {
            ResourceLocked(
              s"File $fileId is locked by a different user",
              f.metadata.lock.map(_.by)
            )
          }
        }

      case _: Ko =>
        Future.successful(NotFound())
    }
  }

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param fid FileId
   * @return a Future containing an Option[File]
   */
  def file(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[File]] = fileRepository.findLatestBy(fid)

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
  ): Future[GetResult[Seq[File]]] = fileRepository.find(filename, maybePath)

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
  ): Future[GetResult[File]] = fileRepository.findLatest(filename, maybePath)

  /**
   * List all the files in the given Folder path for the given OrgId
   *
   * @param path Path
   * @return Option[File]
   */
  def listFiles(path: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[File]]] = fileRepository.listFiles(path)

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
  ): Future[LockResult[Lock]] =
    file(fileId).flatMap {
      case Ok(f) =>
        f.metadata.path.map { path =>
          modifyManagedFile(path)(fileRepository.lock(fileId)) {
            val msg = s"Can't lock file $fileId in a locked sub-tree"
            log.warn(msg)
            ResourceLocked(msg, f.metadata.lock.map(_.by))
          }
        }.getOrElse {
          Future.successful(InvalidData("Missing path"))
        }

      case ko: Ko =>
        Future.successful(ko)
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
  ): Future[LockResult[Lock]] =
    folder(folderId).flatMap {
      case Ok(f) =>
        modifyManagedFile(f.flattenPath)(folderRepository.lock(folderId)) {
          val msg = s"Can't lock file $folderId in a locked sub-tree"
          log.warn(msg)
          ResourceLocked(msg, f.metadata.lock.map(_.by))
        }

      case ko: Ko =>
        Future.successful(ko)
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
  ): Future[UnlockResult[Unit]] =
    file(fileId).flatMap {
      case Ok(f) =>
        f.metadata.path.map { path =>
          modifyManagedFile(path)(fileRepository.unlock(fileId)) {
            val msg = s"Can't unlock file $fileId in a locked sub-tree"
            log.warn(msg)
            ResourceLocked(msg, f.metadata.lock.map(_.by))
          }
        }.getOrElse {
          Future.successful(InvalidData("File did not contain a valid path"))
        }

      case ko: Ko =>
        Future.successful(ko)
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
  ): Future[UnlockResult[Unit]] = folderRepository.unlock(folderId)

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
  ): Future[Boolean] = repo.locked(fid).map(_.map(_.isDefined).getOrElse(false))

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
   * @param uid      UserId
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
  ): Future[Boolean] =
    repo.locked(fid).map(_.map(_.contains(uid)).getOrElse(false))

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
