/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package dman

import models.customer.CustomerId
import models.parties.UserId
import models.project.ProjectId
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import util.mongodb.MongoSpec

class DocumentManagementSpec extends Specification with DmanDummy with MongoSpec {

  sequential

  val cid = CustomerId.create()

  "When managing folders as a user it" should {

    "be possible to create a root folder if one doesn't exist" in {
      createRootFolder(cid).isDefined must_== true
    }

    "not be possible to create a root folder if one exists" in {
      createRootFolder(cid).isEmpty must_== true
    }

    "be possible to create a folder if it doesn't already exist" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")
      val f3 = Path("/bingo")
      val f4 = Path("/bingo/bango")

      createFolder(cid, f1).isDefined must_== true
      createFolder(cid, f2).isDefined must_== true
      createFolder(cid, f3).isDefined must_== true
      createFolder(cid, f4).isDefined must_== true
    }

    "not be possible to create a folder if it already exists" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")

      createFolder(cid, f1).isEmpty must_== true
      createFolder(cid, f2).isEmpty must_== true
    }

    "be possible to get the entire tree from the root folder" in {
      treePaths(cid).size must_== 5
    }

    "be possible to get the sub-tree from a folder" in {
      treePaths(cid, Path("/foo")).size must_== 2
      treePaths(cid, Path("/bingo/bango")).size must_== 1
    }

    "create all parent folders for a folder if they do not exist by default" in {
      val f = Path("/hoo/haa/hii")

      createFolder(cid, f).isDefined must_== true

      val t = treePaths(cid, Path("/hoo"))
      t.size must_== 3
      t.head.path must_== "/root/hoo/"
      t.tail.head.path must_== "/root/hoo/haa/"
      t.last.path must_== "/root/hoo/haa/hii/"
    }

    "not create all parent folders for a folder if so specified" in {
      val f = Path("/yksi/kaksi/myfolder")

      createFolder(cid, f, createMissing = false).isDefined must_== false
      treePaths(cid, Path("/yksi")).size must_== 0
    }

    "be possible to rename a folder" in {
      val orig = Path("/hoo")
      val mod = Path("/huu")

      val res1 = moveFolder(cid, orig, mod)
      res1.size must_== 3
      res1.head.path must_== "/root/huu/"
      res1.tail.head.path must_== "/root/huu/haa/"
      res1.last.path must_== "/root/huu/haa/hii/"

      val res2 = moveFolder(cid, mod, orig)
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
      val fw = file(cid, "test.pdf", Path.root)
      saveFile(uid, fw) must_!= None
    }

    "be possible to save a new file in a sub-folder" in new FileHandlingContext {
      val fw = file(cid, "test.pdf", Path("/hoo/haa"))
      saveFile(uid, fw) must_!= None
    }

    "be possible for a user to lock a file" in new FileHandlingContext {
      val fw = file(cid, "lock-me.pdf", Path("/foo/bar"))

      val maybeFileId = saveFile(uid, fw)
      maybeFileId must_!= None

      val maybeLock = lockFile(uid, maybeFileId.get)
      maybeLock must_!= None
    }

    "not be possible to lock an already locked file" in new FileHandlingContext {
      // Add a file to lock
      val fw = file(cid, "cannot-lock-me-twice.pdf", Path("/foo"))
      val maybeFileId = saveFile(uid, fw)
      maybeFileId must_!= None
      // Lock the file
      lockFile(uid, maybeFileId.get) must_!= None
      // Try to apply a new lock...should not be allowed
      lockFile(uid, maybeFileId.get) must_== None
    }

    "be possible for a user to unlock a file" in new FileHandlingContext {
      // Add a file to lock
      val fw = file(cid, "unlock-me.pdf", Path("/foo"))
      val maybeFileId = saveFile(uid, fw)
      maybeFileId must_!= None
      // Lock the file
      lockFile(uid, maybeFileId.get) must_!= None
      // Try to unlock the file
      unlockFile(uid, maybeFileId.get) must_== true

      val act = getFile(maybeFileId.get)
      act must_!= None
      act.get.metadata.lock must_== None
    }

    "not be possible to unlock a file if the user doesn't own the lock" in new FileHandlingContext {
      // Add a file to lock
      val fw = file(cid, "not-unlockable-me.pdf", Path("/foo"))
      val maybeFileId = saveFile(uid, fw)
      maybeFileId must_!= None
      // Lock the file
      lockFile(uid, maybeFileId.get) must_!= None
      // Try to unlock the file
      unlockFile(UserId.create(), maybeFileId.get) must_== false
    }

    "be possible to look up a list of files in a folder" in {
      val res = listFiles(cid, Path("/foo"))
      res.isEmpty must_== false
      res.size must_== 3
    }

    "be possible to get the entire tree files and their respective folders" in {
      val tree = treeWithFiles(cid)
      tree.isEmpty must_== false
      tree.size should_== 14

      val folders = tree.filter(_.metadata.isFolder.getOrElse(false))
      folders.isEmpty must_== false
      folders.size must_== 8

      val files = tree.filterNot(_.metadata.isFolder.getOrElse(false))
      files.isEmpty must_== false
      files.size must_== 6
    }

    "be possible to lookup a file by the unique file id" in new FileHandlingContext {
      val fw = file(cid, "minion.pdf", Path("/bingo/bango"))
      val maybeFileId = saveFile(uid, fw)
      maybeFileId must_!= None

      val res = getFile(maybeFileId.get)
      res must_!= None
      res.get.filename must_== "minion.pdf"
      res.get.metadata.path.get.path must_== Path("/root/bingo/bango/").path
    }

    "be possible to lookup a file by the filename and folder path" in new FileHandlingContext {
      val res = getLatestFile(cid, "minion.pdf", Some(Path("/bingo/bango")))
      res.size must_!= None
      res.get.filename must_== "minion.pdf"
      res.get.metadata.path.get.path must_== Path("/root/bingo/bango/").path
    }

    "be possible to upload a new version of a file" in new FileHandlingContext {
      val folder = Path("/root/bingo/")
      val fn = "minion.pdf"
      val fw = file(cid, fn, folder)

      // Save the first version
      saveFile(uid, fw) must_!= None
      // Save the second version
      saveFile(uid, fw) must_!= None

      val res2 = getLatestFile(cid, fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.metadata.path.get.path must_== folder.path
      res2.get.metadata.version must_== 2
    }

    "be possible to upload a new version of a file if it is locked by the same user" in new FileHandlingContext {
      val folder = Path("/root/bingo/")
      val fn = "locked-with-version.pdf"
      val fw = file(cid, fn, folder)
      // Save the first version
      val mf1 = saveFile(uid, fw)
      mf1 must_!= None

      // Lock the file
      val maybeLock = lockFile(uid, mf1.get)
      maybeLock must_!= None

      // Save the second version
      saveFile(uid, fw) must_!= None

      val res2 = getLatestFile(cid, fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.metadata.path.get.path must_== folder.path
      res2.get.metadata.version must_== 2
      res2.get.metadata.lock must_== maybeLock
    }

    "not be possible to upload a new version of a file if it is locked by someone else" in new FileHandlingContext {
      val folder = Path("/root/bingo/bango/")
      val fn = "unsaveable-by-another.pdf"
      val fw = file(cid, fn, folder)
      val u2 = UserId.create()
      // Save the first version
      val mf1 = saveFile(uid, fw)
      mf1 must_!= None

      // Lock the file
      val maybeLock = lockFile(uid, mf1.get)
      maybeLock must_!= None

      // Save the second version
      saveFile(u2, fw) must_== None

      val res2 = getLatestFile(cid, fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.metadata.path.get.path must_== folder.path
      res2.get.metadata.version must_== 1
      res2.get.metadata.lock must_== maybeLock
    }

    "be possible to lookup all versions of a file by the filename and folder path" in new FileHandlingContext {
      val folder = Path("/root/bingo/bango/")
      val fn = "multiversion.pdf"
      val fw = file(cid, fn, folder)
      val u2 = UserId.create()
      // Save a few versions of the document
      for (x <- 1 to 5) {
        saveFile(uid, fw)
      }

      val res = getFiles(cid, fn, Some(folder))
      res.size must_== 5
      res.head.filename must_== fn
      res.head.metadata.path.get.path must_== folder.path
      res.head.metadata.version must_== 5
      res.last.metadata.version must_== 1
    }

    "be possible to move a file (including all its previous versions) to a different folder" in {
      val from = Path("/bingo/bango/")
      val to = Path("/hoo/")
      val fn = "multiversion.pdf"

      val original = getFiles(cid, fn, Some(from))

      val res = moveFile(cid, fn, from, to)
      res must_!= None
      res.get.filename must_== fn
      res.get.metadata.path must_!= None
      res.get.metadata.path.get.materialize must_== to.materialize

      getFiles(cid, fn, Some(to)).size must_== original.size
    }
  }
}

trait DmanDummy extends Operations

class FileHandlingContext extends Scope {
  val pid = ProjectId.create()
  val uid = UserId.create()
  val maybeFileStream = Option(this.getClass.getResourceAsStream("/files/test.pdf"))

  def file(cid: CustomerId, fname: String, folder: Path) =
    File(
      filename = fname,
      contentType = Some("application/pdf"),
      stream = maybeFileStream,
      metadata = FileMetadata(
        cid = cid,
        pid = Some(pid),
        uploadedBy = Some(uid),
        path = Some(folder),
        description = Some("This is a test")
      )
    )
}
