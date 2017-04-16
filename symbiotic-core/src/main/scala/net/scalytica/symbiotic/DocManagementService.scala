package net.scalytica.symbiotic

import net.scalytica.symbiotic.core.ConfigResolver.RepoInstance
import net.scalytica.symbiotic.data.CommandStatusTypes._
import net.scalytica.symbiotic.data.Lock.LockOpStatusTypes.LockApplied
import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data._
import org.slf4j.LoggerFactory

/**
 * Singleton object that provides document management operations towards GridFS.
 * Operations allow access to both Folders, which are simple entries in the
 * fs.files collection, and the complete GridFSFile instance including the input
 * stream of the file itself (found in fs.chunks).
 */
final class DocManagementService {

  private val folderRepository = RepoInstance.folderRepository
  private val fileRepository   = RepoInstance.fileRepository
  private val fstreeRepository = RepoInstance.fsTreeRepository

  private val logger = LoggerFactory.getLogger(this.getClass)

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
  def moveFolder(orig: Path, mod: Path)(implicit uid: UserId): Seq[Path] = {
    treeWithFiles(Some(orig)).flatMap { fw =>
      fw.metadata.path.map { f =>
        val upd = Path(f.path.replaceAll(orig.path, mod.path))
        folderRepository.move(f, upd) match {
          case CommandOk(n) =>
            Option(upd)

          case CommandKo(n) =>
            // This case actually can't occur. Since there are
            // repository calls made earlier that will catch the non-existence
            // of the folder. So the code is probably a bit too defensive.
            logger.warn(s"Path ${f.path} was not updated to ${upd.path}")
            None

          case CommandError(n, m) =>
            logger.error(
              s"An error occured when trying to update path" +
                s" ${f.path} to ${upd.path}. Message is: $m"
            )
            None
        }
      }
    }.filter(_.isDefined).flatten
  }

  /**
   * This method will return the a collection of files, representing the
   * folder/directory structure that has been set-up in GridFS.
   *
   * @param from Path location to return the tree structure from. Defaults to rootFolder
   * @return a collection of BaseFile instances that match the criteria
   */
  def treeWithFiles(
      from: Option[Path]
  )(implicit uid: UserId): Seq[ManagedFile] =
    fstreeRepository.tree(from)

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param from Path location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treeNoFiles(from: Option[Path])(implicit uid: UserId): Seq[Folder] =
    fstreeRepository.tree(from).flatMap(Folder.mapTo)

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Paths that match the criteria.
   */
  def treePaths(
      from: Option[Path]
  )(implicit uid: UserId): Seq[(FileId, Path)] =
    fstreeRepository.treePaths(from)

  /**
   * This method will return a collection of File instances , representing the direct descendants
   * for the given Folder.
   *
   * @param from Path location to return the tree structure from. Defaults to rootFolder
   * @return a collection of BaseFile instances that match the criteria
   */
  def childrenWithFiles(
      from: Option[Path]
  )(implicit uid: UserId): Seq[ManagedFile] =
    fstreeRepository.children(from)

  /**
   * Moves a file to another folder if, and only if, the folder doesn't contain
   * a file with the same name.
   *
   * @param filename String
   * @param orig     Path
   * @param mod      Path the folder to place the file
   * @return An Option with the updated File
   */
  def moveFile(
      filename: String,
      orig: Path,
      mod: Path
  )(implicit uid: UserId): Option[File] =
    fileRepository
      .findLatest(filename, Some(mod))
      .fold(
        fileRepository.move(filename, orig, mod)
      ) { _ =>
        logger.info(
          s"Not moving file $filename to $mod because a file with " +
            s"the same name already exists."
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
  def moveFile(
      fileId: FileId,
      orig: Path,
      mod: Path
  )(implicit uid: UserId): Option[File] =
    fileRepository
      .getLatest(fileId)
      .map(fw => moveFile(fw.filename, orig, mod))
      .getOrElse {
        logger.info(s"Could not find file with with id $fileId")
        None
      }

  /**
   * Get the folder with the given FolderId.
   *
   * @param folderId FolderId
   * @return An Option with the found Folder.
   */
  def getFolder(folderId: FolderId)(implicit uid: UserId): Option[Folder] =
    folderRepository.get(folderId)

  /**
   * Attempt to create a folder. If successful it will return the FolderId.
   * If segments of the Folder path is non-existing, these will be created as well.
   *
   * @param at Path to create
   * @return maybe a FileId if it was successfully created
   */
  def createFolder(
      at: Path,
      createMissing: Boolean = true
  )(implicit uid: UserId): Option[FileId] = {
    if (createMissing) {
      logger.debug(s"Creating folder $at")
      val fid = folderRepository.save(Folder(uid, at))
      logger.debug(s"Creating any missing parent folders for $at")
      createNonExistingFoldersInPath(at)
      fid
    } else {
      val verifyPath = at.materialize
        .split(",")
        .filterNot(_.isEmpty)
        .dropRight(1)
        .mkString("/", "/", "/")

      val vf      = Path(verifyPath)
      val missing = folderRepository.filterMissing(vf)
      if (missing.isEmpty) {
        logger.debug(s"Parent folders exist, creating folder $at")
        folderRepository.save(Folder(uid, at))
      } else {
        logger.warn(
          s"Did not create folder because there are missing parent" +
            s" folders for $at."
        )
        None
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
  private def createNonExistingFoldersInPath(
      p: Path
  )(implicit uid: UserId): List[Path] = {
    val missing = folderRepository.filterMissing(p)
    logger.trace(s"Missing folders are: [${missing.mkString(", ")}]")
    missing.foreach(mp => folderRepository.save(Folder(uid, mp)))
    missing
  }

  /**
   * Convenience function for creating the root Folder.
   *
   * @return maybe a FileId if the root folder was created
   */
  def createRootFolder(implicit uid: UserId): Option[FileId] =
    folderRepository.save(Folder.root(uid))

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param at Path with the path to look for
   * @return true if the folder exists, else false
   */
  def folderExists(at: Path)(implicit uid: UserId): Boolean =
    folderRepository.exists(at)

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param uid UserId
   * @param f   File
   * @return Option[FileId]
   */
  def saveFile(f: File)(implicit uid: UserId): Option[FileId] = {
    val dest = f.metadata.path.getOrElse(Path.root)

    if (dest == Path.root && !folderRepository.exists(Path.root))
      createRootFolder

    if (folderRepository.exists(dest)) {
      fileRepository
        .findLatest(f.filename, f.metadata.path)
        .fold(fileRepository.save(f)) { latest =>
          val canSave = latest.metadata.lock.fold(false)(l => l.by == uid)
          if (canSave) {
            val res = fileRepository.save(
              f.copy(
                metadata = f.metadata.copy(
                  version = latest.metadata.version + 1,
                  lock = latest.metadata.lock
                )
              )
            )
            // Unlock the previous version.
            unlockFile(latest.metadata.fid.get)
            res
          } else {
            if (latest.metadata.lock.isDefined)
              logger.warn(
                s"Cannot save file because it is locked by another " +
                  s"user: ${latest.metadata.lock.map(_.by).getOrElse("<NA>")}"
              )
            else
              logger.warn(s"Cannot save file because the file isn't locked.")
            None
          }
        }
    } else {
      logger.warn(
        s"Attempted to save file to non-existing destination " +
          s"folder: ${dest.path}, materialized as ${dest.materialize}"
      )
      None
    }
  }

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[File]
   */
  def getFile(fid: FileId)(implicit uid: UserId): Option[File] =
    fileRepository.getLatest(fid)

  /**
   * Will return a collection of File (if found) with the provided filename and
   * folder properties.
   *
   * @param filename  String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def getFiles(
      filename: String,
      maybePath: Option[Path]
  )(implicit uid: UserId): Seq[File] = fileRepository.find(filename, maybePath)

  /**
   * Will return the latest version of a file (File)
   *
   * @param filename  String
   * @param maybePath Option[Path]
   * @return An Option with a File
   */
  def getLatestFile(
      filename: String,
      maybePath: Option[Path]
  )(implicit uid: UserId): Option[File] =
    fileRepository.findLatest(filename, maybePath)

  /**
   * List all the files in the given Folder path for the given OrgId
   *
   * @param path Path
   * @return Option[File]
   */
  def listFiles(path: Path)(implicit uid: UserId): Seq[File] =
    fileRepository.listFiles(path.materialize)

  /**
   * Places a lock on a file to prevent any modifications or new versions of the
   * file
   *
   * @param uid    UserId The id of the user that places the lock
   * @param fileId FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will
   *         contain the applied lock.
   */
  def lockFile(fileId: FileId)(implicit uid: UserId): Option[Lock] =
    fileRepository.lock(fileId) match {
      case LockApplied(s) => s
      case _              => None
    }

  /**
   * Unlocks the provided file if and only if the provided user is the one
   * holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlockFile(fid: FileId)(implicit uid: UserId): Boolean =
    fileRepository.unlock(fid) match {
      case LockApplied(t) => true
      case _              => false
    }

  /**
   * Checks if the file has a lock or not
   *
   * @param fileId FileId
   * @return true if locked, else false
   */
  def hasLock(fileId: FileId)(implicit uid: UserId): Boolean =
    fileRepository.locked(fileId).isDefined

  /**
   * Checks if the file is locked and if it is locked by the given user
   *
   * @param fileId FileId
   * @param uid    UserId
   * @return true if locked by user, else false
   */
  def isLockedBy(fileId: FileId, uid: UserId): Boolean =
    fileRepository.locked(fileId)(uid).contains(uid)
}
