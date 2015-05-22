/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import test.util.mongodb.MongoSpec

class DocumentManagementSpec extends Specification with DmanDummy with MongoSpec {

  val cid = CustomerId.create()

  "When managing folders as a user it" should {

    "be possible to create a root folder if one doesn't exist" in {
      createRootFolder(cid).isDefined must_== true
    }

    "not be possible to create a root folder if one exists" in {
      createRootFolder(cid).isEmpty must_== true
    }

    "be possible to create a folder if it doesn't already exist" in {
      val f1 = Folder("/foo")
      val f2 = Folder("/foo/bar")
      val f3 = Folder("/bingo")
      val f4 = Folder("/bingo/bango")

      createFolder(cid, f1).isDefined must_== true
      createFolder(cid, f2).isDefined must_== true
      createFolder(cid, f3).isDefined must_== true
      createFolder(cid, f4).isDefined must_== true
    }

    "not be possible to create a folder if it already exists" in {
      val f1 = Folder("/foo")
      val f2 = Folder("/foo/bar")

      createFolder(cid, f1).isEmpty must_== true
      createFolder(cid, f2).isEmpty must_== true
    }

    "be possible to get the entire tree from the root folder" in {
      treeNoFiles(cid).size must_== 5
    }

    "be possible to get the sub-tree from a folder" in {
      treeNoFiles(cid, Folder("/foo")).size must_== 2
      treeNoFiles(cid, Folder("/bingo/bango")).size must_== 1
    }

    "create all parent folders for a folder if they do not exist by default" in {
      val f = Folder("/hoo/haa/hii")

      createFolder(cid, f).isDefined must_== true

      val t = treeNoFiles(cid, Folder("/hoo"))
      t.size must_== 3
      t.head.path must_== "/root/hoo/"
      t.tail.head.path must_== "/root/hoo/haa/"
      t.last.path must_== "/root/hoo/haa/hii/"
    }

    "not create all parent folders for a folder if so specified" in {
      val f = Folder("/yksi/kaksi/myfolder")

      createFolder(cid, f, createMissing = false).isDefined must_== false
      treeNoFiles(cid, Folder("/yksi")).size must_== 0
    }

    "be possible to rename a folder" in {
      val orig = Folder("/hoo")
      val mod = Folder("/huu")

      val res1 = renameFolder(cid, orig, mod)
      res1.size must_== 3
      res1.head.path must_== "/root/huu/"
      res1.tail.head.path must_== "/root/huu/haa/"
      res1.last.path must_== "/root/huu/haa/hii/"

      val res2 = renameFolder(cid, mod, orig)
      res2.size must_== 3
      res2.head.path must_== "/root/hoo/"
      res2.tail.head.path must_== "/root/hoo/haa/"
      res2.last.path must_== "/root/hoo/haa/hii/"
    }

    "be possible to move a folder and its contents" in {
      pending("TODO")
    }
  }

  "To manage files as a user it" should {

    "be possible to save a new file in the root folder" in new FileHandlingContext {
      val fw = fileWrapper(cid, "test.pdf", Folder.rootFolder)
      saveFileWrapper(uid, fw) must_!= None
    }

    "be possible to save a new file in a sub-folder" in new FileHandlingContext {
      val fw = fileWrapper(cid, "test.pdf", Folder("/hoo/haa"))
      saveFileWrapper(uid, fw) must_!= None
    }

    "be possible for a user to lock a file" in new FileHandlingContext {
      val fw = fileWrapper(cid, "lock-me.pdf", Folder("/foo/bar"))

      val maybeFileId = saveFileWrapper(uid, fw)
      maybeFileId must_!= None

      val maybeLock = lockFile(uid, maybeFileId.get)
      maybeLock must_!= None
    }

    "not be possible to lock an already locked file" in new FileHandlingContext {
      // Add a file to lock
      val fw = fileWrapper(cid, "cannot-lock-me-twice.pdf", Folder("/foo"))
      val maybeFileId = saveFileWrapper(uid, fw)
      maybeFileId must_!= None
      // Lock the file
      lockFile(uid, maybeFileId.get) must_!= None
      // Try to apply a new lock...should not be allowed
      lockFile(uid, maybeFileId.get) must_== None
    }

    "be possible for a user to unlock a file" in new FileHandlingContext {
      // Add a file to lock
      val fw = fileWrapper(cid, "unlock-me.pdf", Folder("/foo"))
      val maybeFileId = saveFileWrapper(uid, fw)
      maybeFileId must_!= None
      // Lock the file
      lockFile(uid, maybeFileId.get) must_!= None
      // Try to unlock the file
      unlockFile(uid, maybeFileId.get) must_== true

      val act = getFileWrapper(maybeFileId.get)
      act must_!= None
      act.get.lock must_== None
    }

    "not be possible to unlock a file if the user doesn't own the lock" in new FileHandlingContext {
      // Add a file to lock
      val fw = fileWrapper(cid, "not-unlockable-me.pdf", Folder("/foo"))
      val maybeFileId = saveFileWrapper(uid, fw)
      maybeFileId must_!= None
      // Lock the file
      lockFile(uid, maybeFileId.get) must_!= None
      // Try to unlock the file
      unlockFile(new UserId(new ObjectId().toString), maybeFileId.get) must_== false
    }

    "be possible to look up a list of files in a folder" in {
      val res = listFiles(cid, Folder("/foo"))
      res.isEmpty must_== false
      res.size must_== 3
    }

    "be possible to get the entire tree files and their respective folders" in {
      val tree = treeWithFiles(cid)
      tree.isEmpty must_== false
      tree.size should_== 14

      val folders = tree.filter(_.isFolder.getOrElse(false))
      folders.isEmpty must_== false
      folders.size must_== 8

      val files = tree.filterNot(_.isFolder.getOrElse(false))
      files.isEmpty must_== false
      files.size must_== 6
    }

    "be possible to lookup a file by the unique file id" in new FileHandlingContext {
      val fw = fileWrapper(cid, "minion.pdf", Folder("/bingo/bango"))
      val maybeFileId = saveFileWrapper(uid, fw)
      maybeFileId must_!= None

      val res = getFileWrapper(maybeFileId.get)
      res must_!= None
      res.get.filename must_== "minion.pdf"
      res.get.folder.get.path must_== Folder("/root/bingo/bango/").path
    }

    "be possible to lookup a file by the filename and folder path" in new FileHandlingContext {
      val res = getLatestFileWrapper(cid, "minion.pdf", Some(Folder("/bingo/bango")))
      res.size must_!= None
      res.get.filename must_== "minion.pdf"
      res.get.folder.get.path must_== Folder("/root/bingo/bango/").path
    }

    "be possible to upload a new version of a file" in new FileHandlingContext {
      val folder = Folder("/root/bingo/")
      val fn = "minion.pdf"
      val fw = fileWrapper(cid, fn, folder)

      // Save the first version
      saveFileWrapper(uid, fw) must_!= None
      // Save the second version
      saveFileWrapper(uid, fw) must_!= None

      val res2 = getLatestFileWrapper(cid, fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.folder.get.path must_== folder.path
      res2.get.version must_== 2
    }

    "be possible to upload a new version of a file if it is locked by the same user" in new FileHandlingContext {
      val folder = Folder("/root/bingo/")
      val fn = "locked-with-version.pdf"
      val fw = fileWrapper(cid, fn, folder)
      // Save the first version
      val mf1 = saveFileWrapper(uid, fw)
      mf1 must_!= None

      // Lock the file
      val maybeLock = lockFile(uid, mf1.get)
      maybeLock must_!= None

      // Save the second version
      saveFileWrapper(uid, fw) must_!= None

      val res2 = getLatestFileWrapper(cid, fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.folder.get.path must_== folder.path
      res2.get.version must_== 2
      res2.get.lock must_== maybeLock
    }

    "not be possible to upload a new version of a file if it is locked by someone else" in new FileHandlingContext {
      val folder = Folder("/root/bingo/bango/")
      val fn = "unsaveable-by-another.pdf"
      val fw = fileWrapper(cid, fn, folder)
      val u2 = new UserId(new ObjectId().toString)
      // Save the first version
      val mf1 = saveFileWrapper(uid, fw)
      mf1 must_!= None

      // Lock the file
      val maybeLock = lockFile(uid, mf1.get)
      maybeLock must_!= None

      // Save the second version
      saveFileWrapper(u2, fw) must_== None

      val res2 = getLatestFileWrapper(cid, fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.folder.get.path must_== folder.path
      res2.get.version must_== 1
      res2.get.lock must_== maybeLock
    }

    "be possible to lookup all versions of a file by the filename and folder path" in new FileHandlingContext {
      val folder = Folder("/root/bingo/bango/")
      val fn = "multiversion.pdf"
      val fw = fileWrapper(cid, fn, folder)
      val u2 = new UserId(new ObjectId().toString)
      // Save a few versions of the document
      for (x <- 1 to 5) {
        saveFileWrapper(uid, fw)
      }

      val res = getFileWrappers(cid, fn, Some(folder))
      res.size must_== 5
      res.head.filename must_== fn
      res.head.folder.get.path must_== folder.path
      res.head.version must_== 5
      res.last.version must_== 1
    }

    "be possible to move a file (including all its previous versions) to a different folder" in {
      val from = Folder("/bingo/bango/")
      val to = Folder("/hoo/")
      val fn = "multiversion.pdf"

      val original = getFileWrappers(cid, fn, Some(from))

      val res = moveFile(cid, fn, from, to)
      res must_!= None
      res.get.filename must_== fn
      res.get.folder must_!= None
      res.get.folder.get.materialize must_== to.materialize

      getFileWrappers(cid, fn, Some(to)).size must_== original.size
    }
  }
}

trait DmanDummy extends DocManOperations

class FileHandlingContext extends Scope {
  val pid = ProjectId.create()
  val uid = UserId.create()
  val maybeFileStream = Option(this.getClass.getResourceAsStream("/files/test.pdf"))

  def fileWrapper(cid: CustomerId, fname: String, folder: Folder) =
    FileWrapper(
      filename = fname,
      contentType = Some("application/pdf"),
      stream = maybeFileStream,
      cid = cid,
      pid = Some(pid),
      uploadedBy = Some(uid),
      folder = Some(folder),
      description = Some("This is a test")
    )
}
