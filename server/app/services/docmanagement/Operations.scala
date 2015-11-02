/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.docmanagement

import com.mongodb.casbah.Imports._
import models.docmanagement.CommandStatusTypes.{CommandError, CommandKo, CommandOk}
import models.docmanagement.Lock.LockOpStatusTypes.Success
import models.docmanagement._
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import org.slf4j.LoggerFactory

/**
 * Singleton object that provides document management operations towards GridFS. Operations allow access to both
 * Folders, which are simple entries in the fs.files collection, and the complete GridFSFile instance including the
 * input stream of the file itself (found in fs.chunks).
 */
trait Operations {
  self =>

  val logger = LoggerFactory.getLogger(self.getClass)

  /**
   * Function allowing renaming of folder segments!
   * - Find the tree of folders from the given path element to rename
   * - Update all path segments with the new name for the given path element.
   * - Return all folders that were affected
   *
   * TODO: This should also trigger a re-indexing in the search engine (once that's in place)
   *
   * @param oid OrgId
   * @param orig Path with the original full path
   * @param mod Path with the modified full path
   * @return A collection containing the folder paths that were updated.
   */
  protected def moveFolder(oid: OrganisationId, orig: Path, mod: Path): Seq[Path] = {
    treeWithFiles(oid, orig).flatMap { fw =>
      fw.metadata.path.map { f =>
        val upd = Path(f.path.replaceAll(orig.path, mod.path))
        // TODO: Need to change the _name_ of the folder too
        FolderService.move(oid, f, upd) match {
          case CommandOk(n) => Option(upd)
          case CommandKo(n) =>
            logger.warn(s"Path ${f.path} was not updated to ${upd.path}")
            None
          case CommandError(n, m) =>
            logger.error(s"An error occured when trying to update path ${f.path} to ${upd.path}. Message is: $m")
            None
        }
      }
    }.filter(_.isDefined).flatten
  }

  /**
   * This method will return the a collection of files, representing the folder/directory
   * structure that has been set-up in GridFS.
   *
   * @param oid OrgId
   * @param from Path location to return the tree structure from. Defaults to rootFolder
   * @return a collection of BaseFile instances that match the criteria
   */
  protected def treeWithFiles(oid: OrganisationId, from: Path = Path.root): Seq[BaseFile] =
    FSTree.treeWith[BaseFile](oid, from)(mdbo => BaseFile.fromBSON(mdbo))

  /**
   * This method will return a collection of File instances , representing the direct descendants
   * for the given Folder.
   *
   * @param oid OrgId
   * @param from Path location to return the tree structure from. Defaults to rootFolder
   * @return a collection of BaseFile instances that match the criteria
   */
  protected def childrenWithFiles(oid: OrganisationId, from: Path = Path.root): Seq[BaseFile] =
    FSTree.childrenWith[BaseFile](oid, from)(mdbo => BaseFile.fromBSON(mdbo))

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param oid OrgId
   * @param from Path location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  protected def treeNoFiles(oid: OrganisationId, from: Path = Path.root): Seq[Folder] =
    FSTree.treeWith[Folder](oid, from)(mdbo => Folder.fromBSON(mdbo))

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param oid OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Paths that match the criteria.
   */
  protected def treePaths(oid: OrganisationId, from: Path = Path.root): Seq[Path] = FSTree.treePaths(oid, from)

  /**
   * Moves a file to another folder if, and only if, the folder doesn't contain a file with the same name.
   *
   * @param oid OrgId
   * @param filename String
   * @param orig Path
   * @param mod Path the folder to place the file
   * @return An Option with the updated File
   */
  protected def moveFile(oid: OrganisationId, filename: String, orig: Path, mod: Path): Option[File] = {
    FileService.findLatest(oid, filename, Some(mod)).fold(
      FileService.move(oid, filename, orig, mod)
    ) { _ =>
        logger.info(s"Not moving file $filename to $mod because a file with the same name already exists.")
        None
      }
  }

  protected def moveFile(fileId: FileId, orig: Path, mod: Path): Option[File] = {
    FileService.getLatest(fileId).map(fw => moveFile(fw.metadata.oid, fw.filename, orig, mod)).getOrElse {
      logger.info(s"Could not find file with with id $fileId")
      None
    }
  }

  /**
   * Attempt to create a folder. If successful it will return the FolderId.
   * If segments of the Folder path is non-existing, these will be created as well.
   *
   * @param oid OrgId
   * @param at Path to create
   * @return maybe a FileId if it was successfully created
   */
  protected def createFolder(oid: OrganisationId, at: Path, createMissing: Boolean = true): Option[FileId] = {
    if (createMissing) {
      logger.debug(s"Creating folder $at for $oid")
      val fid = FolderService.save(Folder(oid, at))
      logger.debug(s"Creating any missing parent folders for $at")
      createNonExistingFoldersInPath(oid, at)
      fid
    } else {
      val verifyPath: String = at.materialize.split(",").filterNot(_.isEmpty).dropRight(1).mkString("/", "/", "/")
      val vf = Path(verifyPath)
      val missing = FolderService.filterMissing(oid, vf)
      if (missing.isEmpty) {
        logger.debug(s"Parent folders exist, creating folder $at for $oid")
        FolderService.save(Folder(oid, at))
      } else {
        logger.warn(s"Did not create folder because there are missing parent folders for $at.")
        None
      }
    }
  }

  /**
   * Will create any missing path segments found in the Folder path, and return a List of all the
   * Folders that were created.
   *
   * @param oid OrgId
   * @param p Path to verify path and create non-existing segments
   * @return A List containing the missing folders that were created.
   */
  private def createNonExistingFoldersInPath(oid: OrganisationId, p: Path): List[Path] = {
    val missing = FolderService.filterMissing(oid, p)
    logger.trace(s"Missing folders are: [${missing.mkString(", ")}]")
    missing.foreach(mp => FolderService.save(Folder(oid, mp)))
    missing
  }

  /**
   * Convenience function for creating the root Folder.
   *
   * @param oid OrgId
   * @return maybe a FileId if the root folder was created
   */
  protected def createRootFolder(oid: OrganisationId): Option[FileId] = FolderService.save(Folder.rootFolder(oid))

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param oid OrgId
   * @param at Path with the path to look for
   * @return true if the folder exists, else false
   */
  protected def folderExists(oid: OrganisationId, at: Path): Boolean = FolderService.exists(oid, at)

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param uid UserId
   * @param f File
   * @return Option[FileId]
   */
  protected def saveFile(uid: UserId, f: File): Option[FileId] = {
    val dest = f.metadata.path.getOrElse(Path.root)
    if (FolderService.exists(f.metadata.oid, dest)) {
      FileService.findLatest(f.metadata.oid, f.filename, f.metadata.path).fold(FileService.save(f)) { latest =>
        val canSave = latest.metadata.lock.fold(false)(l => l.by == uid)
        if (canSave) {
          val res = FileService.save(
            f.copy(metadata = f.metadata.copy(version = latest.metadata.version + 1, lock = latest.metadata.lock))
          )
          // Unlock the previous version.
          unlockFile(uid, latest.metadata.fid.get)
          res
        } else {
          if (latest.metadata.lock.isDefined)
            logger.warn(s"Cannot save file because it is locked by another user: ${latest.metadata.lock.get.by}")
          else
            logger.warn(s"Cannot save file because the file isn't locked.")
          None
        }
      }
    } else {
      logger.warn(s"Attempted to save file to non-existing destination folder: ${dest.path}")
      None
    }
  }

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[File]
   */
  protected def getFile(fid: FileId): Option[File] = FileService.getLatest(fid)

  /**
   * Will return a collection of File (if found) with the provided filename and folder properties.
   *
   * @param oid OrgId
   * @param filename String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  protected def getFiles(oid: OrganisationId, filename: String, maybePath: Option[Path]): Seq[File] =
    FileService.find(oid, filename, maybePath)

  /**
   * Will return the latest version of a file (File)
   *
   * @param oid OrgId
   * @param filename String
   * @param maybePath Option[Path]
   * @return An Option with a File
   */
  protected def getLatestFile(oid: OrganisationId, filename: String, maybePath: Option[Path]): Option[File] =
    FileService.findLatest(oid, filename, maybePath)

  /**
   * List all the files in the given Folder path for the given OrgId
   *
   * @param oid OrgId
   * @param path Path
   * @return Option[File]
   */
  protected def listFiles(oid: OrganisationId, path: Path): Seq[File] = FileService.listFiles(oid, path.materialize)

  /**
   * Places a lock on a file to prevent any modifications or new versions of the file
   *
   * @param uid UserId The id of the user that places the lock
   * @param fileId FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will contain the applied lock.
   */
  protected def lockFile(uid: UserId, fileId: FileId): Option[Lock] = FileService.lock(uid, fileId) match {
    case Success(s) => s
    case _ => None
  }

  /**
   * Unlocks the provided file if and only if the provided user is the one holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  protected def unlockFile(uid: UserId, fid: FileId): Boolean = FileService.unlock(uid, fid) match {
    case Success(t) => true
    case _ => false
  }

  /**
   * Checks if the file has a lock or not
   *
   * @param fileId FileId
   * @return true if locked, else false
   */
  protected def hasLock(fileId: FileId): Boolean = FileService.locked(fileId).isDefined

  /**
   * Checks if the file is locked and if it is locked by the given user
   *
   * @param fileId FileId
   * @param uid UserId
   * @return true if locked by user, else false
   */
  protected def isLockedBy(fileId: FileId, uid: UserId): Boolean = FileService.locked(fileId).contains(uid)
}
