package net.scalytica.symbiotic.core

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import net.scalytica.symbiotic.api.types.ResourceParties.{Org, Owner}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.test.generators.FileGenerator.file
import net.scalytica.symbiotic.test.generators.FolderGenerator.createFolder
import net.scalytica.symbiotic.test.generators.{
  TestContext,
  TestOrgId,
  TestUserId
}
import net.scalytica.symbiotic.test.specs.PersistenceSpec
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.Inspectors.forAll

import scala.concurrent.ExecutionContext.Implicits.global

trait DocManagementServiceSpec
    extends WordSpecLike
    with MustMatchers
    with Inside
    with ScalaFutures
    with OptionValues
    with BeforeAndAfterAll { self: PersistenceSpec =>

  // scalastyle:off magic.number

  val cfgResolver: ConfigResolver
  lazy val service = new DocManagementService(cfgResolver)

  val usrId        = TestUserId.create()
  val orgId        = TestOrgId.create()
  val owner        = Owner(orgId, Org)
  implicit val ctx = TestContext(usrId, owner, Seq(owner.id))

  val usrId2 = TestUserId.create()
  val owner2 = Owner(usrId2, Org)
  val ctx2   = TestContext(usrId2, owner2, Seq(owner2.id))

  val folderIds = Seq.newBuilder[FolderId]
  val fileIds   = Seq.newBuilder[FileId]

  def getFolderId(at: Int): FolderId = folderIds.result()(at)

  def getFileId(at: Int): FileId = fileIds.result()(at)

  def getFolderIndex(fid: FolderId): Option[Int] =
    folderIds.result().zipWithIndex.find(fid == _._1).map(_._2)

  def getFileIndex(fid: FileId): Option[Int] =
    fileIds.result().zipWithIndex.find(fid == _._1).map(_._2)

  "When managing files and folders as a user it" should {

    "be possible to create a root folder if one doesn't exist" in {
      val res = service.createRootFolder.futureValue
      res.foreach(folderIds += _)
      res.isDefined mustBe true
    }

    "not be possible to create a root folder if one exists" in {
      service.createRootFolder.futureValue.isDefined mustBe false
    }

    "be possible to create a folder if it doesn't already exist" in {
      val paths = Seq(
        Path("/foo"),
        Path("/foo/bar"),
        Path("/bingo"),
        Path("/bingo/bango")
      )

      forAll(paths) { p =>
        val res = service.createFolder(p).futureValue
        res.foreach(folderIds += _)
        res must not be empty
      }
    }

    "be possible to create a folder with metadata and folder type" in {
      val p  = Path("/bingo/bango/bongo")
      val ft = "custom folder"
      val ea = MetadataMap("extra1" -> "FooBar", "extra2" -> 12.21d)

      val res = service.createFolder(p, Some(ft), Some(ea)).futureValue
      res.foreach(folderIds += _)
      res must not be empty
    }

    "not create a folder with an existing name in the same parent" in {
      val p = Path("/bingo/bango/bongo")

      service.createFolder(p).futureValue mustBe empty
    }

    "find and return a specific folder with metadata and folder type" in {
      val p = Path("/bingo/bango/bongo")
      val f = service.folder(p).futureValue.value

      f.filename mustBe "bongo"
      f.fileType mustBe Some("custom folder")
      f.metadata.owner mustBe Some(owner)
      f.metadata.isFolder mustBe Some(true)
      f.metadata.path mustBe Some(p)
      f.metadata.extraAttributes must not be empty

      val ea = f.metadata.extraAttributes.get
      ea.get("extra1") mustBe Some(StrValue("FooBar"))
      ea.get("extra2") mustBe Some(DoubleValue(12.21d))
    }

    "be possible to update the metadata for a folder" in {
      val p  = Path("/bingo/bango/bongo")
      val f1 = service.folder(p).futureValue.value

      val md1 = f1.metadata
      val ea  = md1.extraAttributes.get.plainMap ++ Map("extra1" -> "FizzBuzz")
      val upd = f1.copy(metadata = md1.copy(extraAttributes = Some(ea)))

      service.updateFolder(upd).futureValue mustBe f1.metadata.fid

      val f2 = service.folder(p).futureValue.value

      f2.filename mustBe f1.filename
      f2.flattenPath mustBe f1.flattenPath
      f2.metadata.fid mustBe md1.fid
      f2.metadata.extraAttributes must not be empty

      val attrs = f2.metadata.extraAttributes.get
      attrs.get("extra1") mustBe Some(StrValue("FizzBuzz"))
      attrs.get("extra2") mustBe md1.extraAttributes.get.get("extra2")
    }

    "not be possible to create a folder if it already exists" in {
      val f1 = Path("/foo")
      val f2 = Path("/foo/bar")

      service.createFolder(f1).futureValue.isEmpty mustBe true
      service.createFolder(f2).futureValue.isEmpty mustBe true
    }

    "allow owners to create folders with same name as other owners" in {
      val path = Path("/foo/bar")

      val res1 = service.createRootFolder(ctx2, global).futureValue
      res1.foreach(folderIds += _)
      res1 must not be empty

      val res2 = service.createFolder(path)(ctx2, global).futureValue
      res2.foreach(folderIds += _)
      res2 must not be empty
    }

    "allow creation of folders with a fully initialised Folder type" in {
      val f = createFolder(orgId, Path.root, "pre-initialised", Some("foobar"))

      val res = service.createFolder(f).futureValue

      res.foreach(folderIds += _)
      res must not be empty
    }

    "be possible to get the entire tree from the root folder" in {
      service.treePaths(None).futureValue.size mustBe 7
    }

    "be possible to get the sub-tree from a folder" in {
      service.treePaths(Some(Path("/foo"))).futureValue.size mustBe 2
      service.treePaths(Some(Path("/bingo/bango"))).futureValue.size mustBe 2
    }

    "create all parent folders for a folder if they don't exist" in {
      val f = Path("/hoo/haa/hii")

      service.createFolder(f).futureValue must not be empty

      val t = service.treePaths(Some(Path("/hoo"))).futureValue

      t.foreach {
        case (fid, _) => folderIds += fid
      }

      t.size mustBe 3
      t.head._2.value mustBe "/root/hoo"
      t.tail.head._2.value mustBe "/root/hoo/haa"
      t.last._2.value mustBe "/root/hoo/haa/hii"
    }

    "not create parent folders when the createMissing argument is false" in {
      val f = Path("/yksi/kaksi/myfolder")

      service
        .createFolder(f, createMissing = false)
        .futureValue
        .isDefined mustBe false
      service.treePaths(Some(Path("/yksi"))).futureValue.size mustBe 0
    }

    "create a new folder in an existing folder" in {
      val f = Path("/bingo/bango/huu")

      val res = service.createFolder(f, createMissing = false).futureValue
      res.foreach(folderIds += _)
      res must not be empty

      service.treePaths(Option(f.parent)).futureValue.size mustBe 3
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
      res1.head.value mustBe "/root/huu"
      res1.tail.head.value mustBe "/root/huu/haa"
      res1.last.value mustBe "/root/huu/haa/hii"

      val res2 = service.moveFolder(mod, orig).futureValue
      res2.size mustBe 3
      res2.head.value mustBe "/root/hoo"
      res2.tail.head.value mustBe "/root/hoo/haa"
      res2.last.value mustBe "/root/hoo/haa/hii"
    }

    "be possible to update metadata for a folder" in {
      val dt        = DateTime.now
      val maybeOrig = service.folder(Path("/bingo/bango/huu")).futureValue
      maybeOrig must not be empty

      inside(maybeOrig) {
        case Some(orig) =>
          val u = orig.copy(
            fileType = Some("updated folder"),
            metadata = orig.metadata.copy(
              extraAttributes = Some(
                MetadataMap(
                  "arg1" -> 123,
                  "arg2" -> dt
                )
              )
            )
          )

          service.updateFolder(u).futureValue must not be empty

          val res = service.folder(orig.metadata.fid.get).futureValue
          res must not be empty
          res.get.fileType mustBe Some("updated folder")
          res.get.metadata.extraAttributes must not be empty

          val ea = res.get.metadata.extraAttributes.get
          ea.get("arg1") mustBe Some(IntValue(123))
          ea.get("arg2") mustBe Some(JodaValue(dt))

        case None =>
          fail("Expected to find a Folder")
      }
    }

    "not do anything if renaming a folder that doesn't exist" in {
      val na  = Path("/hoo/trallallallala")
      val mod = Path("/hoo/lalallalaaa")

      service.moveFolder(na, mod).futureValue.size mustBe 0
    }

    "be possible to get a folder using its FolderId" in {
      val path = Path("/root/red/blue/yellow")

      val mfid = service.createFolder(path).futureValue
      mfid.foreach(folderIds += _)
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

    "be possible to save a new file in the root folder" in {
      val fw  = file(owner, usrId, "test.pdf", Path.root)
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res must not be empty
    }

    "be possible to save a new file in a sub-folder" in {
      val fw  = file(owner, usrId, "test.pdf", Path("/hoo/haa"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res must not be empty
    }

    "be possible for a user to lock a file" in {
      val fw = file(owner, usrId, "lock-me.pdf", Path("/foo/bar"))

      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res must not be empty

      service.lockFile(res.get).futureValue must not be empty
    }

    "not be able to lock an already locked file" in {
      // Add a file to lock
      val fw  = file(owner, usrId, "cannot-lock-me-twice.pdf", Path("/foo"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res must not be empty
      // Lock the file
      service.lockFile(res.get).futureValue must not be empty
      // Try to apply a new lock...should not be allowed
      service.lockFile(res.get).futureValue mustBe None
    }

    "not be possible to move a file that is locked by a different user" in {
      val from = Path("/foo/bar")
      val to   = Path("/hoo/")
      val fn   = "lock-me.pdf"

      val currCtx = ctx.copy(currentUser = usrId2)
      service.moveFile(fn, from, to)(currCtx, global).futureValue mustBe None
    }

    "not be possible to update a file that is locked by a different user" in {
      val path = Path("/foo/")
      val fn   = "cannot-lock-me-twice.pdf"

      val f = service.listFiles(fn, Some(path)).futureValue.headOption.value

      val upd =
        f.copy(metadata = f.metadata.copy(description = Some("modified")))

      val currCtx = ctx.copy(currentUser = usrId2)
      service.updateFile(upd)(currCtx, global).futureValue mustBe None
    }

    "be possible for a user to unlock a file" in {
      // Add a file to lock
      val fw  = file(owner, usrId, "unlock-me.pdf", Path("/foo"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res must not be empty

      // Lock the file
      service.lockFile(res.get).futureValue must not be empty
      // Try to unlock the file
      service.unlockFile(res.get).futureValue mustBe true

      val act = service.file(res.get).futureValue
      act must not be empty
      act.get.metadata.lock mustBe None
    }

    "not be possible to unlock a file if the user doesn't own the lock" in {
      // Add a file to lock
      val fw  = file(owner, usrId, "not-unlockable-me.pdf", Path("/foo"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res must not be empty
      // Lock the file
      service.lockFile(res.get).futureValue must not be empty
      // Try to unlock the file
      val currCtx = ctx.copy(currentUser = usrId2)
      service.unlockFile(res.get)(currCtx, global).futureValue mustBe false
    }

    "be possible to look up a list of files in a folder" in {
      val res = service.listFiles(Path("/foo")).futureValue
      res.isEmpty mustBe false
      res.size mustBe 3
    }

    "be possible to get the entire tree of files and folders" in {
      val tree = service.treeWithFiles(None).futureValue
      tree.isEmpty mustBe false
      tree.size mustBe 20

      val folders = tree.filter(_.metadata.isFolder.getOrElse(false))
      folders.isEmpty mustBe false
      folders.size mustBe 14

      val files = tree.filterNot(_.metadata.isFolder.getOrElse(false))
      files.isEmpty mustBe false
      files.size mustBe 6
    }

    "be possible to get the entire tree of folders without any files" in {
      val tree = service.treeNoFiles(None).futureValue
      tree.isEmpty mustBe false
      tree.size mustBe 14
    }

    "be possible to get all children for a position in the tree" in {
      val from = Path("/foo")
      val children =
        service.childrenWithFiles(Some(from)).futureValue

      children.isEmpty mustBe false
      children.size mustBe 4
    }

    "be possible to lookup a file by the unique file id" in {
      val fw   = file(owner, usrId, "minion.pdf", Path("/bingo/bango"))
      val mfid = service.saveFile(fw).futureValue
      mfid.foreach(fileIds += _)
      mfid must not be empty

      val res = service.file(mfid.get).futureValue
      res must not be empty
      res.get.filename mustBe "minion.pdf"
      res.get.metadata.path.get.value mustBe Path("/root/bingo/bango/").value
    }

    "be possible to lookup a file by the filename and folder path" in {
      val res =
        service.latestFile("minion.pdf", Some(Path("/bingo/bango"))).futureValue
      res must not be empty
      res.get.filename mustBe "minion.pdf"
      res.get.metadata.path.get.value mustBe Path("/root/bingo/bango/").value
    }

    "not be possible to upload new version of a file if it isn't locked" in {
      val folder = Path("/root/bingo/")
      val fn     = "minion.pdf"
      val fw     = file(owner, usrId, fn, folder)

      // Save the first version
      val mfid = service.saveFile(fw).futureValue
      mfid.foreach(fileIds += _)
      mfid must not be empty
      // Save the second version
      service.saveFile(fw).futureValue mustBe None

      val res2 =
        service.latestFile(fn, Some(folder)).futureValue
      res2 must not be empty
      res2.get.filename mustBe fn
      res2.get.metadata.path.get.value mustBe folder.value
      res2.get.metadata.version mustBe 1
    }

    "not be possible moving a file to folder having file with same name" in {
      val orig = Path("/root/foo/bar")
      val dest = Path("/root/bingo/bango")
      val fn   = "minion.pdf"
      val fw   = file(owner, usrId, fn, orig)

      val mfid = service.saveFile(fw).futureValue
      mfid.foreach(fileIds += _)
      mfid must not be empty

      service.moveFile(fn, orig, dest).futureValue mustBe None
    }

    "do nothing when attempting to move a file that doesn't exist" in {
      val orig = Path("/root/foo/bar")
      val dest = Path("/root/bingo/bango")

      service
        .moveFile(FileId(UUID.randomUUID().toString), orig, dest)
        .futureValue mustBe None
    }

    "be possible to add new version of file if locked by the same user" in {
      val folder = Path("/root/bingo/")
      val fn     = "locked-with-version.pdf"
      val fw     = file(owner, usrId, fn, folder)
      // Save the first version
      val mf1 = service.saveFile(fw).futureValue
      mf1.foreach(fileIds += _)
      mf1 must not be empty

      // Lock the file
      val maybeLock = service.lockFile(mf1.get).futureValue
      maybeLock must not be empty

      // Save the second version
      val mf2 = service.saveFile(fw).futureValue
      mf2.foreach(fileIds += _)
      mf2 must not be empty

      val res2 = service.latestFile(fn, Some(folder)).futureValue
      res2 must not be empty
      res2.get.filename mustBe fn
      res2.get.metadata.path.get.value mustBe folder.value
      res2.get.metadata.version mustBe 2
      res2.get.metadata.lock mustBe maybeLock
    }

    "be possible to add a new file without a FileStream" in {
      val folder     = Path("/root/bingo/")
      val fn         = "file-without-filestream.pdf"
      val fw         = file(owner, usrId, fn, folder)
      val fwNoStream = fw.copy(stream = None)
      // Save the first version
      val mf1 = service.saveFile(fwNoStream).futureValue
      mf1.foreach(fileIds += _)
      mf1 must not be empty

      val res1 = service.latestFile(fn, Some(folder)).futureValue
      res1 must not be empty
      res1.get.filename mustBe fn
      res1.get.stream mustBe empty
      res1.get.metadata.path.get.value mustBe folder.value
      res1.get.metadata.version mustBe 1
      res1.get.metadata.lock mustBe empty
    }

    "add new version with a FilesStream to a file prev without FileStream" in {
      val folder     = Path("/root/bingo/")
      val fn         = "file-without-fs-2.pdf"
      val fw         = file(owner, usrId, fn, folder)
      val fwNoStream = fw.copy(stream = None)

      // Save the first version
      val mf1 = service.saveFile(fwNoStream).futureValue
      mf1.foreach(fileIds += _)
      mf1 must not be empty

      val res1 = service.latestFile(fn, Some(folder)).futureValue
      res1 must not be empty
      res1.get.filename mustBe fn
      res1.get.stream mustBe empty
      res1.get.metadata.path.get.value mustBe folder.value
      res1.get.metadata.version mustBe 1
      res1.get.metadata.lock mustBe empty

      // Lock the file
      val maybeLock = service.lockFile(mf1.get).futureValue
      maybeLock must not be empty

      // Save the second version
      val mf2 = service.saveFile(fw).futureValue
      mf2.foreach(fileIds += _)
      mf2 must not be empty

      val res2 = service.latestFile(fn, Some(folder)).futureValue
      res2 must not be empty
      res2.get.filename mustBe fn
      res2.get.metadata.path.get.value mustBe folder.value
      res2.get.metadata.version mustBe 2
      res2.get.metadata.lock mustBe maybeLock
      res2.get.stream must not be empty
    }

    "not be able to upload a new version if  the file is locked by another" in {
      val folder   = Path("/root/bingo/bango/")
      val fn       = "unsaveable-by-another.pdf"
      val fw       = file(owner, usrId, fn, folder)
      val u2       = TestUserId.create()
      val localCtx = TestContext(u2, owner, Seq(owner.id))

      // Save the first version
      val mf1 = service.saveFile(fw).futureValue
      mf1.foreach(fileIds += _)
      mf1 must not be empty

      // Lock the file
      val maybeLock = service.lockFile(mf1.get).futureValue
      maybeLock must not be empty

      // Attempt to save the second version as another user

      service.saveFile(fw)(localCtx, global).futureValue mustBe None

      val res2 = service.latestFile(fn, Some(folder)).futureValue
      res2 must not be empty
      res2.get.filename mustBe fn
      res2.get.metadata.path.get.value mustBe folder.value
      res2.get.metadata.version mustBe 1
      res2.get.metadata.lock mustBe maybeLock
    }

    "be possible to lookup all versions of file by the name and path" in {
      val folder = Path("/root/bingo/bango/")
      val fn     = "multiversion.pdf"
      val fw     = file(owner, usrId, fn, folder)

      // Save a few versions of the document
      val v1 = service.saveFile(fw).futureValue
      v1.foreach(fileIds += _)
      service.lockFile(v1.get).futureValue must not be empty
      for (x <- 1 to 4) {
        service.saveFile(fw).futureValue
      }

      val res = service.listFiles(fn, Some(folder)).futureValue
      res.size mustBe 5
      res.head.filename mustBe fn
      res.head.metadata.path.get.value mustBe folder.value
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

    // Basically preparing for folder locking tests
    "place a new lock on a specific file" in {
      val fid = getFileId(8) // is a deep child of folder with index 1
      service.lockFile(fid).futureValue must not be empty
    }

    "be possible to lock a folder and its entire sub-tree" in {
      val fidToLock = getFolderId(1)

      val res = service.lockFolder(fidToLock).futureValue
      res must not be empty

      res.value.by mustBe usrId
    }

    "return true if the folder is locked" in {
      val fid = getFolderId(1)
      service.folderHasLock(fid).futureValue mustBe true
    }

    "return the true if the folder is locked by the given user" in {
      val fid = getFolderId(1)
      service.folderIsLockedBy(fid, usrId).futureValue mustBe true
    }

    "return false if the folder isn't locked by the given user" in {
      val fid = getFolderId(1)
      service.folderIsLockedBy(fid, usrId2).futureValue mustBe false
    }

    "not be editable if a folder has a locked parent" in {
      val fid = getFolderId(2)
      val f   = service.folder(fid).futureValue.value

      service.canBeEdited(f.flattenPath).futureValue mustBe false
    }

    "be editable if a folder has no locked parent" in {
      val fid = getFolderId(11)
      val f   = service.folder(fid).futureValue.value

      service.canBeEdited(f.flattenPath).futureValue mustBe true
    }

    "prevent moving a file associated with a locked folder" in {
      val fid    = getFileId(8) // is a deep child of folder with index 1
      val before = service.file(fid).futureValue.value
      val orig   = before.metadata.path.get
      val dest   = Path("/bingo/bango/")

      service.moveFile(fid, orig, dest).futureValue mustBe None

      val after = service.file(fid).futureValue.value

      after.metadata.path mustBe before.metadata.path
    }

    "prevent adding a new version of a file if folder tree is locked" in {
      val fid = getFileId(8) // is a deep child of folder with index 1
      val f   = service.file(fid).futureValue.value
      val fw  = file(owner, usrId, f.filename, f.metadata.path.get)

      // Since it's not allowed to place a lock on a file that is already in a
      // locked folder tree, this file has a lock placed on it from a previous
      // test case. It allows to test this service method for correctness.
      service.saveFile(fw).futureValue mustBe None
    }

    "prevent adding a lock on a file if folder tree is locked" in {
      val fid = getFileId(4)
      service.lockFile(fid).futureValue mustBe None
    }

    "prevent moving a folder in the sub-tree of a locked folder" in {
      val fid    = getFolderId(2)
      val before = service.folder(fid).futureValue.value
      val orig   = before.flattenPath
      val dest   = Path("/bingo/bango/")

      service.moveFolder(orig, dest).futureValue mustBe Seq.empty

      val after = service.folder(fid).futureValue.value

      after.flattenPath mustBe before.flattenPath
    }

    "be possible to unlock a folder and its entire sub-tree" in {
      val fid = getFolderId(1)
      service.unlockFolder(fid).futureValue mustBe true
    }

    "still return the full sub-tree" in {
      service.treeWithFiles(Some(Path("/hoo"))).futureValue.size mustBe 5

      service
        .treeWithFiles(Some(Path("/bingo/bango")))
        .futureValue
        .size mustBe 5
    }

    "prevent removing a folder with files in its sub-tree" in {
      val fid = getFolderId(1)

      service.deleteFolder(fid).futureValue.isLeft mustBe true
    }

    "prevent removing a folder with a locked folder in its sub-tree" in {
      val p          = Path("/yes/no/maybe/something/else")
      val lockedPath = p.parent

      // Prepare clean data set
      service.createFolder(p).futureValue must not be empty
      val del = service.folder(Path("/yes")).futureValue.flatMap(_.metadata.fid)
      val lkd = service.folder(lockedPath).futureValue.flatMap(_.metadata.fid)
      service.lockFolder(lkd.value).futureValue must not be empty

      service.deleteFolder(del.value).futureValue.isLeft mustBe true
    }

    "prevent removing a folder in a locked sub-tree" in {
      val p = Path("/yes/no/maybe/something")

      val del = service.folder(p).futureValue.flatMap(_.metadata.fid)
      service.deleteFolder(del.value).futureValue.isLeft mustBe true
    }

    "allow removing a folder with no files or locked folders in sub-tree" in {
      val p       = Path("/abc/def/ghi/jkl/mno")
      val delPath = Path("/abc/def")

      service.createFolder(p).futureValue must not be empty

      val del = service.folder(delPath).futureValue.flatMap(_.metadata.fid)

      service.deleteFolder(del.value).futureValue.isRight mustBe true

      service.folder(del.value).futureValue mustBe empty
    }

    "prevent removing a file locked by a different user" in {
      val p  = Some(Path("/foo/bar"))
      val fn = "lock-me.pdf"

      val currCtx = ctx.copy(currentUser = usrId2)

      val id = service.latestFile(fn, p).futureValue.flatMap(_.metadata.fid)

      service
        .deleteFile(id.value)(currCtx, global)
        .futureValue
        .isLeft mustBe true
    }

    "allow removing a file that is locked by current user" in {
      val p  = Some(Path("/foo/bar"))
      val fn = "lock-me.pdf"

      val id = service.latestFile(fn, p).futureValue.flatMap(_.metadata.fid)

      service.deleteFile(id.value).futureValue.isRight mustBe true

      service.file(id.value).futureValue mustBe empty
    }

    "allow removing a file without lock" in {
      val p  = Some(Path("/foo"))
      val fn = "unlock-me.pdf"

      val id = service.latestFile(fn, p).futureValue.flatMap(_.metadata.fid)

      service.file(id.value).futureValue.value.metadata.lock mustBe empty

      service.deleteFile(id.value).futureValue.isRight mustBe true

      service.file(id.value).futureValue mustBe empty
    }

    "allow erasing a file and all its versions completely from the system" in {
      val fw = file(owner, usrId, "eraseme.pdf", Path("/yes/no"))
      val fw2 =
        (fid: FileId) => fw.copy(metadata = fw.metadata.copy(fid = Some(fid)))

      val fid = service.saveFile(fw).futureValue.value

      service.lockFile(fid).futureValue must not be empty
      service.saveFile(fw2(fid)).futureValue mustBe Some(fid)
      service.saveFile(fw2(fid)).futureValue mustBe Some(fid)
      service.unlockFile(fid).futureValue mustBe true

      service.file(fid).futureValue.map(_.metadata.version) mustBe Some(3)

      service.eraseFile(fid).futureValue.isRight mustBe true
    }

    "be possible to move a folder and all its contents" in {
      val orig = Path("/root/foo")
      val dest = Path("/root/red/blue/foo")

      val origTree = service.treeWithFiles(Some(dest.parent)).futureValue
      val moveTree = service.treeWithFiles(Some(orig)).futureValue

      val res1 = service.moveFolder(orig, dest).futureValue
      res1.size mustBe 2

      val modTree1 = service.treeWithFiles(Some(dest.parent)).futureValue
      modTree1.size mustBe origTree.size + moveTree.size

      // move back
      val res2 = service.moveFolder(dest, orig).futureValue
      res2.size mustBe 2

      val modTree2 = service.treeWithFiles(Some(dest.parent)).futureValue

      modTree2 mustBe origTree
    }

  }

}
