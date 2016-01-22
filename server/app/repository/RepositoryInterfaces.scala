/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository

import java.util.UUID

import core.lib.SuccessOrFailure
import models.base.{Id, ShortName, Username}
import models.docmanagement.CommandStatusTypes.CommandStatus
import models.docmanagement.Lock.LockOpStatusTypes.LockOpStatus
import models.docmanagement._
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.party.{Avatar, Organisation, User}
import models.project.{Member, MemberId, Project, ProjectId}

import scala.reflect.ClassTag

trait UserRepository {

  def save(user: User): SuccessOrFailure

  def findById(id: UserId): Option[User]

  def findByUsername(username: Username): Option[User]

}

trait OrganisationRepository {
  def save(org: Organisation): SuccessOrFailure

  def findById(id: OrganisationId): Option[Organisation]

  def findByShortName(sname: ShortName): Option[Organisation]
}

trait AvatarRepository {
  /**
   * Saves a new Avatar for the User specified in the metadata.
   * Only 1 avatar image per user will be kept, so this method will ensure
   * that old avatar images are cleaned up after adding the new one.
   *
   * @param a the Avatar to save
   * @return an Option that will contain the UUID of the added avatar if successful
   */
  def save(a: Avatar): Option[UUID]

  /**
   * Will return a File (if found) with the provided id.
   *
   * @param uid UserId
   * @return Option[File]
   */
  def get(uid: UserId): Option[Avatar]

  /**
   * Removes _all_ avatar images where filename equals the uid
   *
   * @param uid UserId to remove avatar images for
   */
  def remove(uid: UserId): Unit

  /**
   *
   * @param uid UserId to remove files for.
   * @param ids a collection of the UUID's of files to remove
   */
  def remove(uid: UserId, ids: Seq[UUID]): Unit
}

trait ProjectRepository {
  def save(proj: Project): SuccessOrFailure

  def findById(pid: ProjectId): Option[Project]

  def listByOrgId(oid: OrganisationId): Seq[Project]
}

// TODO: Work in progress...
trait MemberRepository {
  def save(m: Member): Unit // TODO: Make uniform to the other save methods

  def findById(mid: MemberId): Option[Member]

  def listBy[A <: Id](id: A): Seq[Member]

  def listByUserId(uid: UserId): Seq[Member]

  def listByProjectId(pid: ProjectId): Seq[Member]

  def listByOrganisationId(oid: OrganisationId): Seq[Member]
}

trait FileRepository {

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
   * @param id of type java.util.UUID
   * @return Option[File]
   */
  def get(id: UUID): Option[File]

  def getLatest(fid: FileId): Option[File]

  /**
   * "Moves" a file (including all versions) from one folder to another.
   *
   * @param oid      OrgId
   * @param filename String
   * @param orig     Folder
   * @param mod      Folder
   * @return An Option with the updated File
   */
  def move(oid: OrganisationId, filename: String, orig: Path, mod: Path): Option[File]

  /**
   * Will return a collection of File (if found) with the provided filename and folder properties.
   *
   * @param oid       OrgId
   * @param filename  String
   * @param maybePath Option[Path]
   * @return Seq[File]
   */
  def find(oid: OrganisationId, filename: String, maybePath: Option[Path]): Seq[File]

  /**
   * Search for the latest version of a file matching the provided parameters.
   *
   * @param oid       OrgId
   * @param filename  String
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

trait FolderRepository {
  /**
   * Create a new virtual folder in GridFS.
   * If the folder is not defined, the method will attempt to create a root folder if it does not already exist.
   *
   * @param f the folder to add
   * @return An option containing the Id of the created folder, or none if it already exists
   */
  def save(f: Folder): Option[FileId]

  /**
   * Checks for the existence of a Folder
   *
   * @param f Folder
   * @return true if the folder exists, else false
   */
  def exists(f: Folder): Boolean = exists(f.metadata.oid, f.flattenPath)

  /**
   * Checks for the existence of a Path/Folder
   *
   * @param oid OrgId
   * @param at  Path to look for
   * @return true if the folder exists, else false
   */
  def exists(oid: OrganisationId, at: Path): Boolean

  /**
   * Will attempt to identify if any path segments in the provided folders path is missing.
   * If found, a list of the missing Folders will be returned.
   *
   * @param oid OrgId
   * @param p   Path
   * @return list of missing folders
   */
  def filterMissing(oid: OrganisationId, p: Path): List[Path]

  /**
   * This method allows for modifying the path from one value to another.
   * Should only be used in conjunction with the appropriate checks for any child nodes.
   *
   * @param oid  OrgId
   * @param orig FolderPath
   * @param mod  FolderPath
   * @return Option of Int with number of documents affected by the update
   */
  def move(oid: OrganisationId, orig: Path, mod: Path): CommandStatus[Int]
}

trait FSTreeRepository {

  /**
   * Fetch only the Paths for the full folder tree structure, without any file refs.
   *
   * @param oid  OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of Folders that match the criteria.
   */
  def treePaths(oid: OrganisationId, from: Path = Path.root): Seq[Path]

  /**
   * This method will return the a collection of A instances , representing the folder/directory
   * structure that has been set-up in the database.
   *
   * @param oid  OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of A instances
   */
  def tree(oid: OrganisationId, from: Path = Path.root): Seq[ManagedFile]

  /**
   * This method will return the a collection of A instances, representing the direct descendants
   * for the given Folder.
   *
   * @param oid  OrgId
   * @param from Folder location to return the tree structure from. Defaults to rootFolder
   * @return a collection of A instances
   */
  def children(oid: OrganisationId, from: Path = Path.root): Seq[ManagedFile]
}