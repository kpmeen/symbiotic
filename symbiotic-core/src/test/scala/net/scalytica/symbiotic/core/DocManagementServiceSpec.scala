package net.scalytica.symbiotic.core

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{
  File,
  FileId,
  ManagedFileMetadata,
  Path
}
import net.scalytica.symbiotic.test.{MongoSpec, TestUserId}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.specification.mutable.ExecutionEnvironment

import scala.concurrent.Await
import scala.concurrent.duration._

class DocManagementServiceSpec
    extends Specification
    with ExecutionEnvironment
    with MongoSpec {

  sequential

  val timeout: Duration = 2 seconds

  val service = new DocManagementService(new ConfigResolver(config))

  implicit val uid       = TestUserId.create()
  implicit val transform = (s: String) => TestUserId.asId(s)

  // scalastyle:off method.length
  def is(implicit ee: ExecutionEnv) = {

    "When managing files and folders as a user it" should {

      "be possible to create a root folder if one doesn't exist" in {
        Await.result(service.createRootFolder, timeout).isDefined must_== true
      }

      "not be possible to create a root folder if one exists" in {
        Await.result(service.createRootFolder, timeout).isEmpty must_== true
      }

      "be possible to create a folder if it doesn't already exist" in {
        val f1 = Path("/foo")
        val f2 = Path("/foo/bar")
        val f3 = Path("/bingo")
        val f4 = Path("/bingo/bango")

        val r0 = Await.result(service.createFolder(f1), timeout)
        val r1 = Await.result(service.createFolder(f2), timeout)
        val r2 = Await.result(service.createFolder(f3), timeout)
        val r3 = Await.result(service.createFolder(f4), timeout)

        r0.isDefined must_== true
        r1.isDefined must_== true
        r2.isDefined must_== true
        r3.isDefined must_== true
      }

      "not be possible to create a folder if it already exists" in {
        val f1 = Path("/foo")
        val f2 = Path("/foo/bar")

        Await.result(service.createFolder(f1), timeout).isEmpty must_== true
        Await.result(service.createFolder(f2), timeout).isEmpty must_== true
      }

      "be possible to get the entire tree from the root folder" in {
        Await.result(service.treePaths(None), timeout).size must_== 5
      }

      "be possible to get the sub-tree from a folder" in {
        Await
          .result(service.treePaths(Some(Path("/foo"))), timeout)
          .size must_== 2

        Await
          .result(service.treePaths(Some(Path("/bingo/bango"))), timeout)
          .size must_== 1
      }

      "create all parent folders for a folder if they don't exist" in {
        val f = Path("/hoo/haa/hii")

        Await.result(service.createFolder(f), timeout).isDefined must_== true

        val t = Await.result(service.treePaths(Some(Path("/hoo"))), timeout)

        t.size must_== 3
        t.head._2.path must_== "/root/hoo"
        t.tail.head._2.path must_== "/root/hoo/haa"
        t.last._2.path must_== "/root/hoo/haa/hii"
      }

      "not create all parent folders for a folder if so specified" in {
        val f = Path("/yksi/kaksi/myfolder")

        Await
          .result(service.createFolder(f, createMissing = false), timeout)
          .isDefined must_== false
        Await
          .result(service.treePaths(Some(Path("/yksi"))), timeout)
          .size must_== 0
      }

      "create a new folder in an existing folder" in {
        val f = Path("/bingo/bango/huu")

        Await
          .result(service.createFolder(f, createMissing = false), timeout)
          .isDefined must_== true
        Await
          .result(service.treePaths(Option(f.parent)), timeout)
          .size must_== 2
        Await.result(service.treePaths(Option(f)), timeout).size must_== 1
      }

      "confirm that a folder exists" in {
        Await.result(
          service.folderExists(Path("/bingo/bango/huu")),
          timeout
        ) must_== true
      }

      "be possible to rename a folder" in {
        val orig = Path("/hoo")
        val mod  = Path("/huu")

        val res1 = Await.result(service.moveFolder(orig, mod), timeout)
        res1.size must_== 3
        res1.head.path must_== "/root/huu"
        res1.tail.head.path must_== "/root/huu/haa"
        res1.last.path must_== "/root/huu/haa/hii"

        val res2 = Await.result(service.moveFolder(mod, orig), timeout)
        res2.size must_== 3
        res2.head.path must_== "/root/hoo"
        res2.tail.head.path must_== "/root/hoo/haa"
        res2.last.path must_== "/root/hoo/haa/hii"
      }

      "not do anything if renaming a folder that doesn't exist" in {
        val na  = Path("/hoo/trallallallala")
        val mod = Path("/hoo/lalallalaaa")

        Await.result(service.moveFolder(na, mod), timeout).size must_== 0
      }

      "be possible to move a folder and its contents" in {
        pending("TODO")
      }

      "be possible to get a folder using its FolderId" in {
        val path = Path("/root/red/blue/yellow")

        val mfid = Await.result(service.createFolder(path), timeout)
        mfid must_!= None

        val res = Await.result(service.getFolder(mfid.get), timeout)
        res must_!= None
        res.get.filename must_== "yellow"
        res.get.metadata.isFolder must beSome(true)
        res.get.metadata.path must beSome(path)
      }

      "return None when trying to get a folder with a non-existing FileId" in {
        Await.result(service.getFolder(UUID.randomUUID.toString), timeout) must beNone
      }

      "be possible to save a new file in the root folder" in
        new FileHandlingContext {
          val fw = file(uid, "test.pdf", Path.root)
          Await.result(service.saveFile(fw), timeout) must_!= None
        }

      "be possible to save a new file in a sub-folder" in
        new FileHandlingContext {
          val fw = file(uid, "test.pdf", Path("/hoo/haa"))
          Await.result(service.saveFile(fw), timeout) must_!= None
        }

      "be possible for a user to lock a file" in new FileHandlingContext {
        val fw = file(uid, "lock-me.pdf", Path("/foo/bar"))

        val maybeFileId = Await.result(service.saveFile(fw), timeout)
        maybeFileId must_!= None

        Await.result(service.lockFile(maybeFileId.get), timeout) must_!= None
      }

      "not be able to lock an already locked file" in new FileHandlingContext {
        // Add a file to lock
        val fw          = file(uid, "cannot-lock-me-twice.pdf", Path("/foo"))
        val maybeFileId = Await.result(service.saveFile(fw), timeout)
        maybeFileId must_!= None
        // Lock the file
        Await.result(service.lockFile(maybeFileId.get), timeout) must_!= None
        // Try to apply a new lock...should not be allowed
        Await.result(service.lockFile(maybeFileId.get), timeout) must beNone
      }

      "be possible for a user to unlock a file" in new FileHandlingContext {
        // Add a file to lock
        val fw          = file(uid, "unlock-me.pdf", Path("/foo"))
        val maybeFileId = Await.result(service.saveFile(fw), timeout)
        maybeFileId must_!= None

        // Lock the file
        Await.result(service.lockFile(maybeFileId.get), timeout) must_!= None
        // Try to unlock the file
        Await.result(service.unlockFile(maybeFileId.get), timeout) must_== true

        val act = Await.result(service.getFile(maybeFileId.get), timeout)
        act must_!= None
        act.get.metadata.lock must beNone
      }

      "not be possible to unlock a file if the user doesn't own the lock" in
        new FileHandlingContext {
          // Add a file to lock
          val fw       = file(uid, "not-unlockable-me.pdf", Path("/foo"))
          val maybeFid = Await.result(service.saveFile(fw), timeout)
          maybeFid must_!= None
          // Lock the file
          Await.result(service.lockFile(maybeFid.get), timeout) must_!= None
          // Try to unlock the file
          Await.result(
            service
              .unlockFile(maybeFid.get)(TestUserId.create(), transform, ee.ec),
            timeout
          ) must_== false
        }

      "be possible to look up a list of files in a folder" in {
        val res = Await.result(service.listFiles(Path("/foo")), timeout)
        res.isEmpty must_== false
        res.size must_== 3
      }

      "be possible to get the entire tree of files and folders" in {
        val tree = Await.result(service.treeWithFiles(None), timeout)
        tree.isEmpty must_== false
        tree.size should_== 17

        val folders = tree.filter(_.metadata.isFolder.getOrElse(false))
        folders.isEmpty must_== false
        folders.size must_== 12

        val files = tree.filterNot(_.metadata.isFolder.getOrElse(false))
        files.isEmpty must_== false
        files.size must_== 5
      }

      "be possible to get the entire tree of folders without any files" in {
        val tree = Await.result(service.treeNoFiles(None), timeout)
        tree.isEmpty must_== false
        tree.size should_== 12
      }

      "be possible to get all children for a position in the tree" in {
        val from = Path("/foo")
        val children =
          Await.result(service.childrenWithFiles(Some(from)), timeout)

        children.isEmpty must_== false
        children.size must_== 4
      }

      "be possible to lookup a file by the unique file id" in
        new FileHandlingContext {
          val fw          = file(uid, "minion.pdf", Path("/bingo/bango"))
          val maybeFileId = Await.result(service.saveFile(fw), timeout)
          maybeFileId must_!= None

          val res = Await.result(service.getFile(maybeFileId.get), timeout)
          res must_!= None
          res.get.filename must_== "minion.pdf"
          res.get.metadata.path.get.path must_== Path("/root/bingo/bango/").path
        }

      "be possible to lookup a file by the filename and folder path" in
        new FileHandlingContext {
          val res = Await.result(
            service.getLatestFile("minion.pdf", Some(Path("/bingo/bango"))),
            timeout
          )
          res.size must_!= None
          res.get.filename must_== "minion.pdf"
          res.get.metadata.path.get.path must_== Path("/root/bingo/bango/").path
        }

      "not be possible to upload new version of a file if it isn't locked" in
        new FileHandlingContext {
          val folder = Path("/root/bingo/")
          val fn     = "minion.pdf"
          val fw     = file(uid, fn, folder)

          // Save the first version
          Await.result(service.saveFile(fw), timeout) must_!= None
          // Save the second version
          Await.result(service.saveFile(fw), timeout) must beNone

          val res2 =
            Await.result(service.getLatestFile(fn, Some(folder)), timeout)
          res2 must_!= None
          res2.get.filename must_== fn
          res2.get.metadata.path.get.path must_== folder.path
          res2.get.metadata.version must_== 1
        }

      "not be possible to move a file to folder containing file " +
        "with same name" in new FileHandlingContext {
        val orig = Path("/root/foo/bar")
        val dest = Path("/root/bingo/bango")
        val fn   = "minion.pdf"
        val fw   = file(uid, fn, orig)

        Await.result(service.saveFile(fw), timeout) must_!= None

        Await.result(service.moveFile(fn, orig, dest), timeout) must beNone
      }

      "do nothing when attempting to move a file that doesn't exist" in {
        val orig = Path("/root/foo/bar")
        val dest = Path("/root/bingo/bango")

        Await.result(
          service.moveFile(FileId(UUID.randomUUID().toString), orig, dest),
          timeout
        ) must beNone
      }

      "be possible to add new version of file if it is locked by " +
        "the same user" in new FileHandlingContext {
        val folder = Path("/root/bingo/")
        val fn     = "locked-with-version.pdf"
        val fw     = file(uid, fn, folder)
        // Save the first version
        val mf1 = Await.result(service.saveFile(fw), timeout)
        mf1 must_!= None

        // Lock the file
        val maybeLock = Await.result(service.lockFile(mf1.get), timeout)
        maybeLock must_!= None

        // Save the second version
        Await.result(service.saveFile(fw), timeout) must_!= None

        val res2 =
          Await.result(service.getLatestFile(fn, Some(folder)), timeout)
        res2 must_!= None
        res2.get.filename must_== fn
        res2.get.metadata.path.get.path must_== folder.path
        res2.get.metadata.version must_== 2
        res2.get.metadata.lock must_== maybeLock
      }

      "not be possible to upload new version of file if it's locked " +
        "by another" in new FileHandlingContext {
        val folder = Path("/root/bingo/bango/")
        val fn     = "unsaveable-by-another.pdf"
        val fw     = file(uid, fn, folder)
        val u2     = TestUserId.create()

        // Save the first version
        val mf1 = Await.result(service.saveFile(fw), timeout)
        mf1 must_!= None

        // Lock the file
        val maybeLock = Await.result(service.lockFile(mf1.get), timeout)
        maybeLock must_!= None

        // Attempt to save the second version as another user
        Await.result(
          service.saveFile(fw)(u2, transform, ee.ec),
          timeout
        ) must beNone

        val res2 = Await.result(
          service.getLatestFile(fn, Some(folder)),
          timeout
        )
        res2 must_!= None
        res2.get.filename must_== fn
        res2.get.metadata.path.get.path must_== folder.path
        res2.get.metadata.version must_== 1
        res2.get.metadata.lock must_== maybeLock
      }

      "be possible to lookup all versions of file by the name and path" in
        new FileHandlingContext {
          val folder = Path("/root/bingo/bango/")
          val fn     = "multiversion.pdf"
          val fw     = file(uid, fn, folder)

          // Save a few versions of the document
          val v1 = Await.result(service.saveFile(fw), timeout)
          Await.result(service.lockFile(v1.get), timeout) must_!= None
          for (x <- 1 to 4) {
            Await.result(service.saveFile(fw), timeout)
          }

          val res = Await.result(service.getFiles(fn, Some(folder)), timeout)
          res.size must_== 5
          res.head.filename must_== fn
          res.head.metadata.path.get.path must_== folder.path
          res.head.metadata.version must_== 5
          res.last.metadata.version must_== 1
        }

      "be possible to move a file (and all versions) to another folder" in {
        val from = Path("/bingo/bango/")
        val to   = Path("/hoo/")
        val fn   = "multiversion.pdf"

        val original = Await.result(service.getFiles(fn, Some(from)), timeout)

        val res = Await.result(service.moveFile(fn, from, to), timeout)
        res must_!= None
        res.get.filename must_== fn
        res.get.metadata.path must_!= None
        res.get.metadata.path.get.materialize must_== to.materialize

        Await
          .result(service.getFiles(fn, Some(to)), timeout)
          .size must_== original.size
      }
    }
  }
  // scalastyle:on method.length
}

class FileHandlingContext extends Scope {
  val maybeFileStream = Option(
    this.getClass.getResourceAsStream("/files/test.pdf")
  )

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
