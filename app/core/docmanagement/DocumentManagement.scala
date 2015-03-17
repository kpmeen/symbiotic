/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.docmanagement

import core.docmanagement.FileWrapper._
import models.customer.CustomerId
import models.parties.UserId
import org.slf4j.LoggerFactory
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}


/**
 * Singleton object that provides document management operations towards GridFS. Operations allow access to both
 * Folders, which are simple entries in the fs.files collection, and the complete GridFSFile instance including the
 * input stream of the file itself (found in fs.chunks).
 */
object DocumentManagement {

  val logger = LoggerFactory.getLogger(DocumentManagement.getClass)

  /**
   * Ensures that all indices in the <bucket>.files collection are in place
   */
  def ensureIndex(): Unit = FileWrapper.ensureIndex()

  /*
   TODO: Implement function allowing re-naming of folder segments!
    - Find the tree of folders from the given path element to rename
    - Update all path segments with the new name for the given path element.
    - This should also trigger a re-indexing in the search engine (once that's in place)
  */
  def renameFolder(cid: CustomerId, at: Folder) = ???

  /*
    TODO: Implement function for _moving_ a file/folder (same implications as above for rename...same function?)
   */

  /**
   * Attempt to create a folder. If successful it will return the FolderId.
   * If segments of the Folder path is non-existing, these will be created as well.
   *
   * TODO: Make creation optional by passing in a flag to switch on. If not create, check for existence only.
   *
   * @param cid CustomerId
   * @param at Folder to create
   * @return maybe a FolderId if it was successfully created
   */
  def createFolder(cid: CustomerId, at: Folder): Option[FolderId] = {
    val fid = Folder.save(cid, at)
    createNonExistingFoldersInPath(cid, at)
    fid
  }

  /**
   * Convenience function for creating the root Folder.
   *
   * @param cid CustomerId
   * @return maybe a FolderId if the root folder was created
   */
  def createRootFolder(cid: CustomerId): Option[FolderId] = Folder.save(cid)

  /**
   * Will create any missing path segments found in the Folder path, and return a List of all the
   * Folders that were created.
   *
   * @param cid CustomerId
   * @param f Folder to verify path and create non-existing segments
   * @return A List containing the missing folders that were created.
   */
  private def createNonExistingFoldersInPath(cid: CustomerId, f: Folder): List[Folder] = {
    val missing = Folder.filterMissing(cid, f)
    missing.foreach(mf => Folder.save(cid, mf))
    missing
  }

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param cid CustomerId
   * @param at Folder with the path to look for
   * @return true if the folder exists, else false
   */
  def folderExists(cid: CustomerId, at: Folder): Boolean = Folder.exists(cid, at)

  /**
   * This method will return the a collection of FileWrapper instances , representing the folder/directory
   * structure that has been set-up in GridFS.
   *
   * @param cid CustomerId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of FileWrapper instances that match the criteria
   */
  def treeWithFiles(cid: CustomerId, from: Folder = Folder.rootFolder): Seq[FileWrapper] =
    Folder.treeWith[FileWrapper](cid, from)(mdbo => FileWrapper.fromDBObject(mdbo))

  /**
   * Fetch the full folder tree structure without any file refs.
   *
   * @param cid CustomerId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treeNoFiles(cid: CustomerId, from: Folder = Folder.rootFolder): Seq[Folder] = Folder.treeNoFiles(cid, from)

  /**
   * Saves the passed on FileWrapper in MongoDB GridFS
   *
   * @param uid UserId
   * @param f FileWrapper
   * @return Option[ObjectId]
   */
  def save(uid: UserId, f: FileWrapper): Option[FileId] = {
    val dest = f.folder.getOrElse(Folder.rootFolder)
    if (Folder.exists(f.cid, dest)) {
      FileWrapper.findLatest(f.cid, f.filename, f.folder).fold(FileWrapper.save(f)) { latest =>
        val canSave = latest.lock.fold(true)(l => l.by == uid)
        if (canSave) {
          val res = FileWrapper.save(f.copy(version = latest.version + 1, lock = latest.lock))
          // Unlock the previous version. TODO: is this necessary I wonder?
          unlockFile(uid, latest.id.get)
          res
        } else {
          logger.warn(s"Cannot save file because it is locked by another user: ${latest.lock.get.by}")
          None
        }
      }
    } else {
      logger.warn(s"Attempted to save file to non-existing destination folder: ${dest.dematerialize}")
      None
    }
  }

  /**
   * Will return a FileWrapper (if found) with the provided id.
   *
   * @param fid FileId
   * @return Option[FileWrapper]
   */
  def getFileWrapper(fid: FileId): Option[FileWrapper] = FileWrapper.get(fid)

  /**
   * Will return a collection of FileWrapper (if found) with the provided filename and folder properties.
   *
   * @param cid CustomerId
   * @param filename String
   * @param maybePath Option[Folder]
   * @return Seq[FileWrapper]
   */
  def getFileWrappers(cid: CustomerId, filename: String, maybePath: Option[Folder]): Seq[FileWrapper] =
    FileWrapper.find(cid, filename, maybePath)

  /**
   * Will return the latest version of a file (FileWrapper)
   *
   * @param cid CustomerId
   * @param filename String
   * @param maybePath Option[Folder]
   * @return An Option with a FileWrapper
   */
  def getLatestFileWrapper(cid: CustomerId, filename: String, maybePath: Option[Folder]): Option[FileWrapper] =
    FileWrapper.findLatest(cid, filename, maybePath)

  /**
   * List all the files in the given Folder path for the given CustomerId
   *
   * @param cid CustomerId
   * @param folder Folder
   * @return Option[FileWrapper]
   */
  def listFiles(cid: CustomerId, folder: Folder): Seq[FileWrapper] = FileWrapper.listFiles(cid, folder.materialize)


  /**
   * Places a lock on a file to prevent any modifications or new versions of the file
   *
   * @param uid UserId The id of the user that places the lock
   * @param file FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will contain the applied lock.
   */
  def lockFile(uid: UserId, file: FileId): Option[Lock] = FileWrapper.lock(uid, file)

  /**
   * Unlocks the provided file if and only if the provided user is the one holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlockFile(uid: UserId, fid: FileId) = FileWrapper.unlock(uid, fid)

  /**
   * Checks if the file has a lock or not
   *
   * @param file FileId
   * @return true if locked, else false
   */
  def hasLock(file: FileId): Boolean = FileWrapper.locked(file).isDefined

  /**
   * Checks if the file is locked and if it is locked by the given user
   *
   * @param file FileId
   * @param uid UserId
   * @return true if locked by user, else false
   */
  def isLockedBy(file: FileId, uid: UserId): Boolean = locked(file).contains(uid)

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param file FileWrapper
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok)
   */
  def serve(file: FileWrapper)(implicit ec: ExecutionContext): Result = FileWrapper.serve(file)

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param maybeFile Option[FileWrapper]
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok or NotFound)
   */
  def serve(maybeFile: Option[FileWrapper])(implicit ec: ExecutionContext): Result = FileWrapper.serve(maybeFile)

  /**
   * Serves a Future file by streaming the content back as chunks to the client.
   *
   * @param futureFile Future[FileWrapper]
   * @param ec ExecutionContext required due to using Futures
   * @return Future[Result] (Ok)
   */
  def serve(futureFile: Future[FileWrapper])(implicit ec: ExecutionContext): Future[Result] = FileWrapper.serve(futureFile)
}
