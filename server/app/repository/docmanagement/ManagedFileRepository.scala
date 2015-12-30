/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.docmanagement

import models.docmanagement.Lock.LockOpStatusTypes.LockOpStatus
import models.docmanagement.{File, FileId, Lock, Path}
import models.party.PartyBaseTypes.{OrganisationId, UserId}

import scala.reflect.ClassTag

trait ManagedFileRepository {

  /**
   * Saves the passed on File in MongoDB GridFS
   *
   * @param f File
   * @return Option[FileId]
   */
  def save(f: File): Option[FileId]

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param id of type A
   * @return Option[File]
   */
  def get[A](id: A): Option[File]

  def getLatest(fid: FileId): Option[File]

  /**
   * "Moves" a file (including all versions) from one folder to another.
   *
   * @param oid OrgId
   * @param filename String
   * @param orig Folder
   * @param mod Folder
   * @return An Option with the updated File
   */
  def move(oid: OrganisationId, filename: String, orig: Path, mod: Path): Option[File]

  /**
   * Will return a collection of File (if found) with the provided filename and folder properties.
   *
   * @param oid OrgId
   * @param filename String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def find(oid: OrganisationId, filename: String, maybePath: Option[Path]): Seq[File]

  /**
   * Search for the latest version of a file matching the provided parameters.
   *
   * @param oid OrgId
   * @param filename String
   * @param maybePath Option[Folder]
   * @return An Option containing the latest version of the File
   */
  def findLatest(oid: OrganisationId, filename: String, maybePath: Option[Path]): Option[File]

  /**
   * List all the files in the given Folder path
   *
   * @param path String
   * @return Option[File]
   */
  def listFiles(oid: OrganisationId, path: String): Seq[File]

  /**
   * Check if a file is locked or not.
   *
   * @param fid FileId
   * @return an Option with the UserId of the user holding the lock
   */
  def locked(fid: FileId): Option[UserId]

  //  def lockedAnd[A, ID](fid: FileId)(f: (Option[UserId], ID) => A): Option[A] =
  //    getLatest(fid).map(file => f(file.metadata.lock.map(_.by), file.id.get))

  /**
   * Places a lock on a file to prevent any modifications or new versions of the file
   *
   * @param uid UserId The id of the user that places the lock
   * @param fid FileId of the file to lock
   * @return Option[Lock] None if no lock was applied, else the Option will contain the applied lock.
   */
  def lock(uid: UserId, fid: FileId): LockOpStatus[_ <: Option[Lock]]

  /**
   * Unlocks the provided file if and only if the provided user is the one holding the current lock.
   *
   * @param uid UserId
   * @param fid FileId
   * @return
   */
  def unlock(uid: UserId, fid: FileId): LockOpStatus[_ <: String]
}
