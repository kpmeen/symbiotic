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
    TODO: Implement function for _moving_ a file/folder (same impliciations as above for rename...same function?)
   */

  /**
   * TODO: Document me
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
   * TODO: Document me
   *
   * @param cid CustomerId
   * @return maybe a FolderId if the root folder was created
   */
  def createRootFolder(cid: CustomerId): Option[FolderId] = Folder.save(cid)

  /**
   * TODO: Document me
   *
   * @param cid CustomerId
   * @param f Folder to verify path and create non-existing segments
   * @return A List containing the missing folders that were created.
   */
  def createNonExistingFoldersInPath(cid: CustomerId, f: Folder): List[Folder] = {
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
   * TODO: Ensure that the Folder path in question actually exists
   * TODO: Ensure that the version number increases if the filename and path is the same!
   *
   * @param f FileWrapper
   * @return Option[ObjectId]
   */
  def save(f: FileWrapper): Option[FileId] = FileWrapper.save(f)

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
   * @param maybePath Option[Path]
   * @return Seq[FileWrapper]
   */
  def getFileWrappers(cid: CustomerId, filename: String, maybePath: Option[Folder]): Seq[FileWrapper] = FileWrapper.find(cid, filename, maybePath)

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
  def hasLock(file: FileId): Boolean = FileWrapper.locked(file)

  /**
   * Checks if the file is locked and if it is locked by the given user
   *
   * @param file FileId
   * @param uid UserId
   * @return true if locked by user, else false
   */
  def isLockedBy(file: FileId, uid: UserId): Boolean = locked(file, Some(uid))

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
