package net.scalytica.symbiotic.core

import java.util.UUID

import akka.stream.scaladsl.StreamConverters
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{
  File,
  FileId,
  ManagedFileMetadata,
  Path
}
import net.scalytica.symbiotic.test.generators.TestUserId
import net.scalytica.symbiotic.test.specs.PersistenceSpec
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global

trait DocManagementServiceSpec
    extends WordSpecLike
    with MustMatchers
    with ScalaFutures
    with BeforeAndAfterAll { self: PersistenceSpec =>

  val service: DocManagementService

  implicit val uid       = TestUserId.create()
  implicit val transform = (s: String) => TestUserId.asId(s)

  "When managing files and folders as a user it" should {

    "be possible to create a root folder if one doesn't exist" in {
      service.createRootFolder.futureValue.isDefined mustBe true
    }

    "not be possible to create a root folder if one exists" in {
      service.createRootFolder.futureValue.isEmpty mustBe true
    }

    "be possible to create a folder if it doesn't already exist" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")
      val f3 = Path("/bingo")
      val f4 = Path("/bingo/bango")

      service.createFolder(f1).futureValue must not be empty
      service.createFolder(f2).futureValue must not be empty
      service.createFolder(f3).futureValue must not be empty
      service.createFolder(f4).futureValue must not be empty
    }

    "not be possible to create a folder if it already exists" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")

      service.createFolder(f1).futureValue.isEmpty mustBe true
      service.createFolder(f2).futureValue.isEmpty mustBe true
    }

    "be possible to get the entire tree from the root folder" in {
      service.treePaths(None).futureValue.size mustBe 5
    }

    "be possible to get the sub-tree from a folder" in {
      service.treePaths(Some(Path("/foo"))).futureValue.size mustBe 2
      service.treePaths(Some(Path("/bingo/bango"))).futureValue.size mustBe 1
    }

    "create all parent folders for a folder if they don't exist" in {
      val f = Path("/hoo/haa/hii")

      service.createFolder(f).futureValue.isDefined mustBe true

      val t = service.treePaths(Some(Path("/hoo"))).futureValue

      t.size mustBe 3
      t.head._2.path mustBe "/root/hoo"
      t.tail.head._2.path mustBe "/root/hoo/haa"
      t.last._2.path mustBe "/root/hoo/haa/hii"
    }

    "not create all parent folders for a folder if so specified" in {
      val f = Path("/yksi/kaksi/myfolder")

      service
        .createFolder(f, createMissing = false)
        .futureValue
        .isDefined mustBe false
      service.treePaths(Some(Path("/yksi"))).futureValue.size mustBe 0
    }

    "create a new folder in an existing folder" in {
      val f = Path("/bingo/bango/huu")

      service
        .createFolder(f, createMissing = false)
        .futureValue
        .isDefined mustBe true
      service.treePaths(Option(f.parent)).futureValue.size mustBe 2
      service.treePaths(Option(f)).futureValue.size mustBe 1
    }

    "confirm that a folder exists" in {
      service.folderExists(Path("/bingo/bango/huu")).futureValue mustBe true
    }

    "be possible to rename a folder" in {
      val orig = Path("/hoo")
      val mod  = Path("/huu")

      val res1 = service.moveFolder(orig, mod).futureValue
      res1.size mustBe 3
      res1.head.path mustBe "/root/huu"
      res1.tail.head.path mustBe "/root/huu/haa"
      res1.last.path mustBe "/root/huu/haa/hii"

      val res2 = service.moveFolder(mod, orig).futureValue
      res2.size mustBe 3
      res2.head.path mustBe "/root/hoo"
      res2.tail.head.path mustBe "/root/hoo/haa"
      res2.last.path mustBe "/root/hoo/haa/hii"
    }

    "not do anything if renaming a folder that doesn't exist" in {
      val na  = Path("/hoo/trallallallala")
      val mod = Path("/hoo/lalallalaaa")

      service.moveFolder(na, mod).futureValue.size mustBe 0
    }

    "be possible to move a folder and its contents" in {
      pending
    }

    "be possible to get a folder using its FolderId" in {
      val path = Path("/root/red/blue/yellow")

      val mfid = service.createFolder(path).futureValue
      mfid must not be empty

      val res = service.folder(mfid.get).futureValue
      res must not be empty
      res.get.filename mustBe "yellow"
      res.get.metadata.isFolder mustBe Some(true)
      res.get.metadata.path mustBe Some(path)
    }

    "return None when trying to get a folder with a non-existing FileId" in {
      service.folder(UUID.randomUUID.toString).futureValue mustBe None
    }

    "be possible to save a new file in the root folder" in
      new FileHandlingContext {
        val fw = file(uid, "test.pdf", Path.root)
        service.saveFile(fw).futureValue must not be empty
      }

    "be possible to save a new file in a sub-folder" in
      new FileHandlingContext {
        val fw = file(uid, "test.pdf", Path("/hoo/haa"))
        service.saveFile(fw).futureValue must not be empty
      }

    "be possible for a user to lock a file" in new FileHandlingContext {
      val fw = file(uid, "lock-me.pdf", Path("/foo/bar"))

      val maybeFileId = service.saveFile(fw).futureValue
      maybeFileId must not be empty

      service.lockFile(maybeFileId.get).futureValue must not be empty
    }

    "not be able to lock an already locked file" in new FileHandlingContext {
      // Add a file to lock
      val fw          = file(uid, "cannot-lock-me-twice.pdf", Path("/foo"))
      val maybeFileId = service.saveFile(fw).futureValue
      maybeFileId must not be empty
      // Lock the file
      service.lockFile(maybeFileId.get).futureValue must not be empty
      // Try to apply a new lock...should not be allowed
      service.lockFile(maybeFileId.get).futureValue mustBe None
    }

    "be possible for a user to unlock a file" in new FileHandlingContext {
      // Add a file to lock
      val fw          = file(uid, "unlock-me.pdf", Path("/foo"))
      val maybeFileId = service.saveFile(fw).futureValue
      maybeFileId must not be empty

      // Lock the file
      service.lockFile(maybeFileId.get).futureValue must not be empty
      // Try to unlock the file
      service.unlockFile(maybeFileId.get).futureValue mustBe true

      val act = service.file(maybeFileId.get).futureValue
      act must not be empty
      act.get.metadata.lock mustBe None
    }

    "not be possible to unlock a file if the user doesn't own the lock" in
      new FileHandlingContext {
        // Add a file to lock
        val fw       = file(uid, "not-unlockable-me.pdf", Path("/foo"))
        val maybeFid = service.saveFile(fw).futureValue
        maybeFid must not be empty
        // Lock the file
        service.lockFile(maybeFid.get).futureValue must not be empty
        // Try to unlock the file
        service
          .unlockFile(maybeFid.get)(TestUserId.create(), transform, global)
          .futureValue mustBe false
      }

    "be possible to look up a list of files in a folder" in {
      val res = service.listFiles(Path("/foo")).futureValue
      res.isEmpty mustBe false
      res.size mustBe 3
    }

    "be possible to get the entire tree of files and folders" in {
      val tree = service.treeWithFiles(None).futureValue
      tree.isEmpty mustBe false
      tree.size mustBe 18

      val folders = tree.filter(_.metadata.isFolder.getOrElse(false))
      folders.isEmpty mustBe false
      folders.size mustBe 12

      val files = tree.filterNot(_.metadata.isFolder.getOrElse(false))
      files.isEmpty mustBe false
      files.size mustBe 6
    }

    "be possible to get the entire tree of folders without any files" in {
      val tree = service.treeNoFiles(None).futureValue
      tree.isEmpty mustBe false
      tree.size mustBe 12
    }

    "be possible to get all children for a position in the tree" in {
      val from = Path("/foo")
      val children =
        service.childrenWithFiles(Some(from)).futureValue

      children.isEmpty mustBe false
      children.size mustBe 4
    }

    "be possible to lookup a file by the unique file id" in
      new FileHandlingContext {
        val fw          = file(uid, "minion.pdf", Path("/bingo/bango"))
        val maybeFileId = service.saveFile(fw).futureValue
        maybeFileId must not be empty

        val res = service.file(maybeFileId.get).futureValue
        res must not be empty
        res.get.filename mustBe "minion.pdf"
        res.get.metadata.path.get.path mustBe Path("/root/bingo/bango/").path
      }

    "be possible to lookup a file by the filename and folder path" in
      new FileHandlingContext {
        val res =
          service
            .latestFile("minion.pdf", Some(Path("/bingo/bango")))
            .futureValue
        res must not be empty
        res.get.filename mustBe "minion.pdf"
        res.get.metadata.path.get.path mustBe Path("/root/bingo/bango/").path
      }

    "not be possible to upload new version of a file if it isn't locked" in
      new FileHandlingContext {
        val folder = Path("/root/bingo/")
        val fn     = "minion.pdf"
        val fw     = file(uid, fn, folder)

        // Save the first version
        service.saveFile(fw).futureValue must not be empty
        // Save the second version
        service.saveFile(fw).futureValue mustBe None

        val res2 =
          service.latestFile(fn, Some(folder)).futureValue
        res2 must not be empty
        res2.get.filename mustBe fn
        res2.get.metadata.path.get.path mustBe folder.path
        res2.get.metadata.version mustBe 1
      }

    "not be possible to move a file to folder containing file " +
      "with same name" in new FileHandlingContext {
      val orig = Path("/root/foo/bar")
      val dest = Path("/root/bingo/bango")
      val fn   = "minion.pdf"
      val fw   = file(uid, fn, orig)

      service.saveFile(fw).futureValue must not be empty

      service.moveFile(fn, orig, dest).futureValue mustBe None
    }

    "do nothing when attempting to move a file that doesn't exist" in {
      val orig = Path("/root/foo/bar")
      val dest = Path("/root/bingo/bango")

      service
        .moveFile(FileId(UUID.randomUUID().toString), orig, dest)
        .futureValue mustBe None
    }

    "be possible to add new version of file if it is locked by " +
      "the same user" in new FileHandlingContext {
      val folder = Path("/root/bingo/")
      val fn     = "locked-with-version.pdf"
      val fw     = file(uid, fn, folder)
      // Save the first version
      val mf1 = service.saveFile(fw).futureValue
      mf1 must not be empty

      // Lock the file
      val maybeLock = service.lockFile(mf1.get).futureValue
      maybeLock must not be empty

      // Save the second version
      val mf2 = service.saveFile(fw).futureValue
      mf2 must not be empty

      val res2 = service.latestFile(fn, Some(folder)).futureValue
      res2 must not be empty
      res2.get.filename mustBe fn
      res2.get.metadata.path.get.path mustBe folder.path
      res2.get.metadata.version mustBe 2
      res2.get.metadata.lock mustBe maybeLock
    }

    "not be possible to upload new version of file if it's locked " +
      "by another" in new FileHandlingContext {
      val folder = Path("/root/bingo/bango/")
      val fn     = "unsaveable-by-another.pdf"
      val fw     = file(uid, fn, folder)
      val u2     = TestUserId.create()

      // Save the first version
      val mf1 = service.saveFile(fw).futureValue
      mf1 must not be empty

      // Lock the file
      val maybeLock = service.lockFile(mf1.get).futureValue
      maybeLock must not be empty

      // Attempt to save the second version as another user

      service.saveFile(fw)(u2, transform, global).futureValue mustBe None

      val res2 = service.latestFile(fn, Some(folder)).futureValue
      res2 must not be empty
      res2.get.filename mustBe fn
      res2.get.metadata.path.get.path mustBe folder.path
      res2.get.metadata.version mustBe 1
      res2.get.metadata.lock mustBe maybeLock
    }

    "be possible to lookup all versions of file by the name and path" in
      new FileHandlingContext {
        val folder = Path("/root/bingo/bango/")
        val fn     = "multiversion.pdf"
        val fw     = file(uid, fn, folder)

        // Save a few versions of the document
        val v1 = service.saveFile(fw).futureValue
        service.lockFile(v1.get).futureValue must not be empty
        for (x <- 1 to 4) {
          service.saveFile(fw).futureValue
        }

        val res = service.listFiles(fn, Some(folder)).futureValue
        res.size mustBe 5
        res.head.filename mustBe fn
        res.head.metadata.path.get.path mustBe folder.path
        res.head.metadata.version mustBe 5
        res.last.metadata.version mustBe 1
      }

    "be possible to move a file (and all versions) to another folder" in {
      val from = Path("/bingo/bango/")
      val to   = Path("/hoo/")
      val fn   = "multiversion.pdf"

      val original = service.listFiles(fn, Some(from)).futureValue

      val res = service.moveFile(fn, from, to).futureValue
      res must not be empty
      res.get.filename mustBe fn
      res.get.metadata.path must not be empty
      res.get.metadata.path.get.materialize mustBe to.materialize

      service.listFiles(fn, Some(to)).futureValue.size mustBe original.size
    }
  }
}

trait FileHandlingContext {
  val maybeFileStream = Option(
    StreamConverters.fromInputStream(
      () => this.getClass.getResourceAsStream("/files/test.pdf")
    )
  )

  def file(uid: UserId, fname: String, folder: Path) =
    File(
      filename = fname,
      contentType = Some("application/pdf"),
      uploadDate = Some(DateTime.now),
      stream = maybeFileStream,
      metadata = ManagedFileMetadata(
        owner = Some(uid),
        uploadedBy = Some(uid),
        path = Some(folder),
        description = Some("This is a test")
      )
    )
}
