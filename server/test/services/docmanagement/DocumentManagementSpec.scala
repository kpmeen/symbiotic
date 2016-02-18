/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.docmanagement

import models.docmanagement.{File, ManagedFileMetadata, Path}
import models.party.PartyBaseTypes.UserId
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import repository.mongodb.docmanagement.{MongoDBFSTreeRepository, MongoDBFileRepository, MongoDBFolderRepository}
import util.mongodb.MongoSpec

class DocumentManagementSpec extends Specification with DmanDummy with MongoSpec {

  sequential

  implicit val uid = UserId.create()

  "When managing folders as a user it" should {

    "be possible to create a root folder if one doesn't exist" in {
      service.createRootFolder.isDefined must_== true
    }

    "not be possible to create a root folder if one exists" in {
      service.createRootFolder.isEmpty must_== true
    }

    "be possible to create a folder if it doesn't already exist" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")
      val f3 = Path("/bingo")
      val f4 = Path("/bingo/bango")

      service.createFolder(f1).isDefined must_== true
      service.createFolder(f2).isDefined must_== true
      service.createFolder(f3).isDefined must_== true
      service.createFolder(f4).isDefined must_== true
    }

    "not be possible to create a folder if it already exists" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")

      service.createFolder(f1).isEmpty must_== true
      service.createFolder(f2).isEmpty must_== true
    }

    "be possible to get the entire tree from the root folder" in {
      service.treePaths(None).size must_== 5
    }

    "be possible to get the sub-tree from a folder" in {
      service.treePaths(Some(Path("/foo"))).size must_== 2
      service.treePaths(Some(Path("/bingo/bango"))).size must_== 1
    }

    "create all parent folders for a folder if they do not exist by default" in {
      val f = Path("/hoo/haa/hii")

      service.createFolder(f).isDefined must_== true

      val t = service.treePaths(Some(Path("/hoo")))
      t.size must_== 3
      t.head.path must_== "/root/hoo"
      t.tail.head.path must_== "/root/hoo/haa"
      t.last.path must_== "/root/hoo/haa/hii"
    }

    "not create all parent folders for a folder if so specified" in {
      val f = Path("/yksi/kaksi/myfolder")

      service.createFolder(f, createMissing = false).isDefined must_== false
      service.treePaths(Some(Path("/yksi"))).size must_== 0
    }

    "be possible to rename a folder" in {
      val orig = Path("/hoo")
      val mod = Path("/huu")

      val res1 = service.moveFolder(orig, mod)
      res1.size must_== 3
      res1.head.path must_== "/root/huu"
      res1.tail.head.path must_== "/root/huu/haa"
      res1.last.path must_== "/root/huu/haa/hii"

      val res2 = service.moveFolder(mod, orig)
      res2.size must_== 3
      res2.head.path must_== "/root/hoo"
      res2.tail.head.path must_== "/root/hoo/haa"
      res2.last.path must_== "/root/hoo/haa/hii"
    }

    "be possible to move a folder and its contents" in {
      pending("TODO")
    }
  }

  "To manage files as a user it" should {

    "be possible to save a new file in the root folder" in new FileHandlingContext {
      val fw = file(uid, "test.pdf", Path.root)
      service.saveFile(fw) must_!= None
    }

    "be possible to save a new file in a sub-folder" in new FileHandlingContext {
      val fw = file(uid, "test.pdf", Path("/hoo/haa"))
      service.saveFile(fw) must_!= None
    }

    "be possible for a user to lock a file" in new FileHandlingContext {
      val fw = file(uid, "lock-me.pdf", Path("/foo/bar"))

      val maybeFileId = service.saveFile(fw)
      maybeFileId must_!= None

      val maybeLock = service.lockFile(maybeFileId.get)
      maybeLock must_!= None
    }

    "not be possible to lock an already locked file" in new FileHandlingContext {
      // Add a file to lock
      val fw = file(uid, "cannot-lock-me-twice.pdf", Path("/foo"))
      val maybeFileId = service.saveFile(fw)
      maybeFileId must_!= None
      // Lock the file
      service.lockFile(maybeFileId.get) must_!= None
      // Try to apply a new lock...should not be allowed
      service.lockFile(maybeFileId.get) must_== None
    }

    "be possible for a user to unlock a file" in new FileHandlingContext {
      // Add a file to lock
      val fw = file(uid, "unlock-me.pdf", Path("/foo"))
      val maybeFileId = service.saveFile(fw)
      maybeFileId must_!= None

      // Lock the file
      service.lockFile(maybeFileId.get) must_!= None
      // Try to unlock the file
      service.unlockFile(maybeFileId.get) must_== true

      val act = service.getFile(maybeFileId.get)
      act must_!= None
      act.get.metadata.lock must_== None
    }

    "not be possible to unlock a file if the user doesn't own the lock" in new FileHandlingContext {
      // Add a file to lock
      val fw = file(uid, "not-unlockable-me.pdf", Path("/foo"))
      val maybeFileId = service.saveFile(fw)
      maybeFileId must_!= None
      // Lock the file
      service.lockFile(maybeFileId.get) must_!= None
      // Try to unlock the file
      service.unlockFile(maybeFileId.get)(UserId.create()) must_== false
    }

    "be possible to look up a list of files in a folder" in {
      val res = service.listFiles(Path("/foo"))
      res.isEmpty must_== false
      res.size must_== 3
    }

    "be possible to get the entire tree files and their respective folders" in {
      val tree = service.treeWithFiles(None)
      tree.isEmpty must_== false
      tree.size should_== 13

      val folders = tree.filter(_.metadata.isFolder.getOrElse(false))
      folders.isEmpty must_== false
      folders.size must_== 8

      val files = tree.filterNot(_.metadata.isFolder.getOrElse(false))
      files.isEmpty must_== false
      files.size must_== 5
    }

    "be possible to lookup a file by the unique file id" in new FileHandlingContext {
      val fw = file(uid, "minion.pdf", Path("/bingo/bango"))
      val maybeFileId = service.saveFile(fw)
      maybeFileId must_!= None

      val res = service.getFile(maybeFileId.get)
      res must_!= None
      res.get.filename must_== "minion.pdf"
      res.get.metadata.path.get.path must_== Path("/root/bingo/bango/").path
    }

    "be possible to lookup a file by the filename and folder path" in new FileHandlingContext {
      val res = service.getLatestFile("minion.pdf", Some(Path("/bingo/bango")))
      res.size must_!= None
      res.get.filename must_== "minion.pdf"
      res.get.metadata.path.get.path must_== Path("/root/bingo/bango/").path
    }

    "not be possible to upload a new version of a file if the user hasn't locked it" in new FileHandlingContext {
      val folder = Path("/root/bingo/")
      val fn = "minion.pdf"
      val fw = file(uid, fn, folder)

      // Save the first version
      service.saveFile(fw) must_!= None
      // Save the second version
      service.saveFile(fw) must_== None

      val res2 = service.getLatestFile(fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.metadata.path.get.path must_== folder.path
      res2.get.metadata.version must_== 1
    }

    "be possible to upload a new version of a file if it is locked by the same user" in new FileHandlingContext {
      val folder = Path("/root/bingo/")
      val fn = "locked-with-version.pdf"
      val fw = file(uid, fn, folder)
      // Save the first version
      val mf1 = service.saveFile(fw)
      mf1 must_!= None

      // Lock the file
      val maybeLock = service.lockFile(mf1.get)
      maybeLock must_!= None

      // Save the second version
      service.saveFile(fw) must_!= None

      val res2 = service.getLatestFile(fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.metadata.path.get.path must_== folder.path
      res2.get.metadata.version must_== 2
      res2.get.metadata.lock must_== maybeLock
    }

    "not be possible to upload a new version of a file if it is locked by someone else" in new FileHandlingContext {
      val folder = Path("/root/bingo/bango/")
      val fn = "unsaveable-by-another.pdf"
      val fw = file(uid, fn, folder)
      val u2 = UserId.create()
      // Save the first version
      val mf1 = service.saveFile(fw)
      mf1 must_!= None

      // Lock the file
      val maybeLock = service.lockFile(mf1.get)
      maybeLock must_!= None

      // Attempt to save the second version as another user
      service.saveFile(fw)(u2) must_== None

      val res2 = service.getLatestFile(fn, Some(folder))
      res2 must_!= None
      res2.get.filename must_== fn
      res2.get.metadata.path.get.path must_== folder.path
      res2.get.metadata.version must_== 1
      res2.get.metadata.lock must_== maybeLock
    }

    "be possible to lookup all versions of a file by the filename and folder path" in new FileHandlingContext {
      val folder = Path("/root/bingo/bango/")
      val fn = "multiversion.pdf"
      val fw = file(uid, fn, folder)

      // Save a few versions of the document
      val v1 = service.saveFile(fw)
      service.lockFile(v1.get) must_!= None
      for (x <- 1 to 4) {
        service.saveFile(fw)
      }

      val res = service.getFiles(fn, Some(folder))
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

      val original = service.getFiles(fn, Some(from))

      val res = service.moveFile(fn, from, to)
      res must_!= None
      res.get.filename must_== fn
      res.get.metadata.path must_!= None
      res.get.metadata.path.get.materialize must_== to.materialize

      service.getFiles(fn, Some(to)).size must_== original.size
    }
  }
}

trait DmanDummy {

  val service = new DocManagementService(
    folderRepository = new MongoDBFolderRepository(),
    fileRepository = new MongoDBFileRepository(),
    fstreeRepository = new MongoDBFSTreeRepository()
  )

}

class FileHandlingContext extends Scope {
  val maybeFileStream = Option(this.getClass.getResourceAsStream("/files/test.pdf"))

  def file(uid: UserId, fname: String, folder: Path) =
    File(
      filename = fname,
      contentType = Some("application/pdf"),
      stream = maybeFileStream,
      metadata = ManagedFileMetadata(
        owner = Some(uid),
        uploadedBy = Some(uid),
        path = Some(folder),
        description = Some("This is a test")
      )
    )
}
