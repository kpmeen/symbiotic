/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.docmanagement

import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import util.mongodb.MongoSpec

class DocumentManagementSpec extends Specification with MongoSpec {

  val cid = new CustomerId(new ObjectId())

  "When managing folders it" should {

    "be possible to create a root folder if one doesn't exist" in {
      val fid = DocumentManagement.createRootFolder(cid)
      fid.isDefined must_== true
    }
    "not be possible to create a root folder if one exists" in {
      val fid2 = DocumentManagement.createRootFolder(cid)
      fid2.isEmpty must_== true
    }
    "be possible to create a folder if it doesn't already exist" in {
      val f1 = Folder("/foo")
      val fid1 = DocumentManagement.createFolder(cid, f1)
      fid1.isDefined must_== true

      val f2 = Folder("/foo/bar")
      val fid2 = DocumentManagement.createFolder(cid, f2)
      fid2.isDefined must_== true

      val f3 = Folder("/bingo")
      val fid3 = DocumentManagement.createFolder(cid, f3)
      fid3.isDefined must_== true

      val f4 = Folder("/bingo/bango")
      val fid4 = DocumentManagement.createFolder(cid, f4)
      fid4.isDefined must_== true
    }
    "not be possible to create a folder if it already exists" in {
      val f1 = Folder("/foo")
      val fid1 = DocumentManagement.createFolder(cid, f1)
      fid1.isEmpty must_== true

      val f2 = Folder("/foo/bar")
      val fid2 = DocumentManagement.createFolder(cid, f2)
      fid2.isEmpty must_== true
    }
    "be possible to get the entire tree from the root folder" in {
      val t = DocumentManagement.treeNoFiles(cid)
      t.size must_== 5
    }
    "be possible to get the sub-tree from a folder" in {
      val t1 = DocumentManagement.treeNoFiles(cid, Folder("/foo"))
      t1.size must_== 2

      val t2 = DocumentManagement.treeNoFiles(cid, Folder("/bingo/bango"))
      t2.size must_== 1
    }
    "create all parent folders for a folder if they do not exist" in {
      val f = Folder("/hoo/haa/hii")
      val fid = DocumentManagement.createFolder(cid, f)
      fid.isDefined must_== true

      val t = DocumentManagement.treeNoFiles(cid, Folder("/hoo"))
      t.size must_== 3
      t.head.dematerialize must_== "/root/hoo/"
      t.tail.head.dematerialize must_== "/root/hoo/haa/"
      t.last.dematerialize must_== "/root/hoo/haa/hii/"
    }
  }

  "To manage files it" should {

    "be possible to save a new file in the root folder" in new FileHandlingContext {
      val fw = fileWrapper(cid, "test.pdf", Folder.rootFolder)

      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None
    }
    "be possible to save a new file in a sub-folder" in new FileHandlingContext {
      val fw = fileWrapper(cid, "test.pdf", Folder("/hoo/haa"))

      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None
    }
    "be possible for a user to lock a file" in new FileHandlingContext {
      val fw = fileWrapper(cid, "lock-me.pdf", Folder("/foo/bar"))

      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None

      val maybeLock = DocumentManagement.lockFile(uid, maybeFileId.get)
      maybeLock must_!= None
    }
    "not be possible to lock an already locked file" in new FileHandlingContext {
      // Add a file to lock
      val fw = fileWrapper(cid, "cannot-lock-me-twice.pdf", Folder("/foo"))
      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None
      // Lock the file
      val maybeLock = DocumentManagement.lockFile(uid, maybeFileId.get)
      maybeLock must_!= None
      // Try to apply a new lock...should not be allowed
      val noneFileId = DocumentManagement.lockFile(uid, maybeFileId.get)
      noneFileId must_== None
    }
    "be possible for a user to unlock a file" in new FileHandlingContext {
      // Add a file to lock
      val fw = fileWrapper(cid, "unlock-me.pdf", Folder("/foo"))
      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None
      // Lock the file
      val maybeLock = DocumentManagement.lockFile(uid, maybeFileId.get)
      maybeLock must_!= None
      // Try to unlock the file
      val unlocked = DocumentManagement.unlockFile(uid, maybeFileId.get)
      unlocked must_== true

      val act = DocumentManagement.getFileWrapper(maybeFileId.get)
      act must_!= None
      act.get.lock must_== None
    }
    "not be possible to unlock a file if the user doesn't own the lock" in new FileHandlingContext {
      // Add a file to lock
      val fw = fileWrapper(cid, "not-unlockable-me.pdf", Folder("/foo"))
      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None
      // Lock the file
      val maybeLock = DocumentManagement.lockFile(uid, maybeFileId.get)
      maybeLock must_!= None
      // Try to unlock the file
      val unlocked = DocumentManagement.unlockFile(new UserId(new ObjectId()), maybeFileId.get)
      unlocked must_== false
    }
    "be possible to look up a list of files in a folder" in {
      val res = DocumentManagement.listFiles(cid, Folder("/foo"))

      res.isEmpty must_== false
      res.size must_== 3
    }
    "be possible to get the entire tree files and their respective folders" in {
      val tree = DocumentManagement.treeWithFiles(cid)
      tree.isEmpty must_== false
      tree.size should_== 14
    }
    "be possible to lookup a file by the unique file id" in new FileHandlingContext {
      val fw = fileWrapper(cid, "minion.pdf", Folder("/bingo/bango"))
      val maybeFileId = DocumentManagement.save(fw)
      maybeFileId must_!= None

      val res = DocumentManagement.getFileWrapper(maybeFileId.get)
      res must_!= None
      res.get.filename.get must_== "minion.pdf"
      res.get.folder.get.dematerialize must_== Folder("/root/bingo/bango/").dematerialize
    }
    "be possible to lookup a file by the filename and folder path" in new FileHandlingContext {
      val res = DocumentManagement.getFileWrappers(cid, "minion.pdf", Some(Folder("/bingo/bango")))
      res.size must_== 1
      res.head.filename.get must_== "minion.pdf"
      res.head.folder.get.dematerialize must_== Folder("/root/bingo/bango/").dematerialize
    }
    "be possible to upload a new version of a file" in {
      pending("TODO")
    }
    "not be possible to upload a new version of a file if it is locked by someone else" in {
      pending("TODO")
    }
  }
}

class FileHandlingContext extends Scope {
  val pid = new ProjectId(new ObjectId())
  val uid = new UserId(new ObjectId())
  val maybeFileStream = Option(this.getClass.getResourceAsStream("/files/test.pdf"))

  def fileWrapper(cid: CustomerId, fname: String, folder: Folder) =
    FileWrapper(
      filename = Some(fname),
      contentType = Some("application/pdf"),
      stream = maybeFileStream,
      cid = cid,
      pid = Some(pid),
      uploadedBy = Some(uid),
      folder = Some(folder),
      description = Some("This is a test")
    )
}
