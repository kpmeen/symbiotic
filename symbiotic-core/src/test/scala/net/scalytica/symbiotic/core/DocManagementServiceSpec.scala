package net.scalytica.symbiotic.core

import java.util.UUID

import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import net.scalytica.symbiotic.api.types.ResourceParties.{Org, Owner}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.config.ConfigResolver
import net.scalytica.symbiotic.test.generators.FileGenerator.file
import net.scalytica.symbiotic.test.generators.FolderGenerator.createFolder
import net.scalytica.symbiotic.test.generators.{
  TestContext,
  TestOrgId,
  TestUserId
}
import net.scalytica.symbiotic.test.specs.PersistenceSpec
import net.scalytica.symbiotic.test.utils.SymResValues
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global

trait DocManagementServiceSpec
    extends WordSpecLike
    with MustMatchers
    with Inside
    with ScalaFutures
    with OptionValues
    with SymResValues
    with BeforeAndAfterAll {
  self: PersistenceSpec =>

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
      res.success mustBe true
    }

    "not be possible to create a root folder if one exists" in {
      service.createRootFolder.futureValue mustBe a[IllegalDestination]
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
        res.success mustBe true
      }
    }

    "be possible to create a folder with metadata and folder type" in {
      val p  = Path("/bingo/bango/bongo")
      val ft = "custom folder"
      val ea = MetadataMap("extra1" -> "FooBar", "extra2" -> 12.21d)

      val res = service.createFolder(p, Some(ft), Some(ea)).futureValue
      res.foreach(folderIds += _)
      res.success mustBe true
    }

    "not create a folder with an existing name in the same parent" in {
      val p = Path("/bingo/bango/bongo")

      service.createFolder(p).futureValue mustBe an[InvalidData]
    }

    "find and return a specific folder with metadata and folder type" in {
      val p = Path("/bingo/bango/bongo")
      val f = service.folder(p).futureValue.value

      f.filename mustBe "bongo"
      f.fileType mustBe Some("custom folder")
      f.metadata.owner mustBe Some(owner)
      f.metadata.isFolder mustBe true
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

      service.updateFolder(upd).futureValue.toOption mustBe f1.metadata.fid

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

      service.createFolder(f1).futureValue mustBe an[InvalidData]
      service.createFolder(f2).futureValue mustBe an[InvalidData]
    }

    "allow owners to create folders with same name as other owners" in {
      val path = Path("/foo/bar")

      val res1 = service.createRootFolder(ctx2, global).futureValue
      res1.foreach(folderIds += _)
      res1.success mustBe true

      val res2 = service.createFolder(path)(ctx2, global).futureValue
      res2.foreach(folderIds += _)
      res2.success mustBe true
    }

    "allow creation of folders with a fully initialised Folder type" in {
      val f = createFolder(orgId, Path.root, "pre-initialised", Some("foobar"))

      val res = service.createFolder(f).futureValue

      res.foreach(folderIds += _)
      res.success mustBe true
    }

    "be possible to get the entire tree from the root folder" in {
      service.treePaths(None).futureValue.value.size mustBe 7
    }

    "be possible to get the sub-tree from a folder" in {
      service.treePaths(Some(Path("/foo"))).futureValue.value.size mustBe 2
      service
        .treePaths(Some(Path("/bingo/bango")))
        .futureValue
        .value
        .size mustBe 2
    }

    "create all parent folders for a folder if they don't exist" in {
      val f = Path("/hoo/haa/hii")

      service.createFolder(f).futureValue.success mustBe true

      val t = service.treePaths(Some(Path("/hoo"))).futureValue.value

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
        .futureValue mustBe an[InvalidData]
      service.treePaths(Some(Path("/yksi"))).futureValue.value.size mustBe 0
    }

    "create a new folder in an existing folder" in {
      val f = Path("/bingo/bango/huu")

      val res = service.createFolder(f, createMissing = false).futureValue
      res.foreach(folderIds += _)
      res.success mustBe true

      service.treePaths(Option(f.parent)).futureValue.value.size mustBe 3
      service.treePaths(Option(f)).futureValue.value.size mustBe 1
    }

    "confirm that a folder exists" in {
      service.folderExists(Path("/bingo/bango/huu")).futureValue mustBe true
    }

    "be possible to rename a folder" in {
      val orig = Path("/hoo")
      val mod  = Path("/huu")

      val res1 = service.moveFolder(orig, mod).futureValue.value
      res1.size mustBe 3
      res1.head.value mustBe "/root/huu"
      res1.tail.head.value mustBe "/root/huu/haa"
      res1.last.value mustBe "/root/huu/haa/hii"

      val res2 = service.moveFolder(mod, orig).futureValue.value
      res2.size mustBe 3
      res2.head.value mustBe "/root/hoo"
      res2.tail.head.value mustBe "/root/hoo/haa"
      res2.last.value mustBe "/root/hoo/haa/hii"
    }

    "be possible to update metadata for a folder" in {
      val dt   = DateTime.now
      val orig = service.folder(Path("/bingo/bango/huu")).futureValue.value

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

      service.updateFolder(u).futureValue.success mustBe true

      val res = service.folder(orig.metadata.fid.get).futureValue.value
      res.fileType mustBe Some("updated folder")
      res.metadata.extraAttributes must not be empty

      val ea = res.metadata.extraAttributes.get
      ea.get("arg1") mustBe Some(IntValue(123))
      ea.get("arg2") mustBe Some(JodaValue(dt))
    }

    "not do anything if renaming a folder that doesn't exist" in {
      val na  = Path("/hoo/trallallallala")
      val mod = Path("/hoo/lalallalaaa")

      service.moveFolder(na, mod).futureValue mustBe NotModified()
    }

    "be possible to get a folder using its FolderId" in {
      val path = Path("/root/red/blue/yellow")

      val fid = service.createFolder(path).futureValue.value
      folderIds += fid

      val res = service.folder(fid).futureValue.value
      res.filename mustBe "yellow"
      res.metadata.isFolder mustBe true
      res.metadata.path mustBe Some(path)
    }

    "return None when trying to get a folder with a non-existing FileId" in {
      service.folder(UUID.randomUUID.toString).futureValue mustBe NotFound()
    }

    "be possible to save a new file in the root folder" in {
      val fw  = file(owner, usrId, "test.pdf", Path.root)
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res.success mustBe true
    }

    "be possible to save a new file in a sub-folder" in {
      val fw  = file(owner, usrId, "test.pdf", Path("/hoo/haa"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res.success mustBe true
    }

    "be possible for a user to lock a file" in {
      val fw = file(owner, usrId, "lock-me.pdf", Path("/foo/bar"))

      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res.success mustBe true

      service.lockFile(res.get).futureValue.success mustBe true
    }

    "not be able to lock an already locked file" in {
      // Add a file to lock
      val fw  = file(owner, usrId, "cannot-lock-me-twice.pdf", Path("/foo"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res.success mustBe true
      // Lock the file
      service.lockFile(res.get).futureValue.success mustBe true
      // Try to apply a new lock...should not be allowed
      service.lockFile(res.get).futureValue match {
        case ResourceLocked(msg, mp) =>
          mp mustBe Some(usrId)

        case err =>
          fail(s"Expected ResourceLocked but got ${err.getClass}")
      }
    }

    "not be possible to move a file that is locked by a different user" in {
      val from = Path("/foo/bar")
      val to   = Path("/hoo/")
      val fn   = "lock-me.pdf"

      val currCtx = ctx.copy(currentUser = usrId2)
      service.moveFile(fn, from, to)(currCtx, global).futureValue match {
        case ResourceLocked(msg, mp) =>
          mp mustBe Some(usrId)

        case err =>
          fail(s"Expected ResourceLocked but got ${err.getClass}")
      }
    }

    "not be possible to update a file that is locked by a different user" in {
      val path = Path("/foo/")
      val fn   = "cannot-lock-me-twice.pdf"

      val f =
        service.listFiles(fn, Some(path)).futureValue.value.headOption.value

      val upd =
        f.copy(metadata = f.metadata.copy(description = Some("modified")))

      val currCtx = ctx.copy(currentUser = usrId2)
      service.updateFile(upd)(currCtx, global).futureValue match {
        case ResourceLocked(msg, mp) =>
          mp mustBe Some(usrId)

        case err =>
          fail(s"Expected ResourceLocked but got ${err.getClass}")
      }
    }

    "be possible for a user to unlock a file" in {
      // Add a file to lock
      val fw  = file(owner, usrId, "unlock-me.pdf", Path("/foo"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res.success mustBe true

      // Lock the file
      service.lockFile(res.get).futureValue.success mustBe true
      // Try to unlock the file
      service.unlockFile(res.get).futureValue mustBe Ok(())

      val act = service.file(res.get).futureValue.value
      act.metadata.lock mustBe None
    }

    "not be possible to unlock a file if the user doesn't own the lock" in {
      // Add a file to lock
      val fw  = file(owner, usrId, "not-unlockable-me.pdf", Path("/foo"))
      val res = service.saveFile(fw).futureValue
      res.foreach(fileIds += _)
      res.success mustBe true
      // Lock the file
      service.lockFile(res.get).futureValue.success mustBe true
      // Try to unlock the file
      val currCtx = ctx.copy(currentUser = usrId2)
      val rl      = service.unlockFile(res.get)(currCtx, global).futureValue

      rl match {
        case ResourceLocked(msg, by) =>
          by mustBe Some(usrId)

        case err =>
          fail(s"Expected a ResourceLocked by got ${err.getClass}")
      }
    }

    "be possible to look up a list of files in a folder" in {
      service.listFiles(Path("/foo")).futureValue.value.size mustBe 3
    }

    "be possible to get the entire tree of files and folders" in {
      val tree = service.treeWithFiles(None).futureValue.value
      tree.size mustBe 20

      tree.count(_.metadata.isFolder) mustBe 14
      tree.count(f => !f.metadata.isFolder) mustBe 6
    }

    "be possible to get the entire tree of folders without any files" in {
      service.treeNoFiles(None).futureValue.value.size mustBe 14
    }

    "be possible to get all children for a position in the tree" in {
      val from = Path("/foo")
      service.childrenWithFiles(Some(from)).futureValue.value.size mustBe 4
    }

    "be possible to lookup a file by the unique file id" in {
      val fw   = file(owner, usrId, "minion.pdf", Path("/bingo/bango"))
      val mfid = service.saveFile(fw).futureValue
      mfid.foreach(fileIds += _)
      mfid.success mustBe true

      val res = service.file(mfid.get).futureValue.value
      res.filename mustBe "minion.pdf"
      res.metadata.path.get.value mustBe Path("/root/bingo/bango/").value
    }

    "be possible to lookup a file by the filename and folder path" in {
      val res =
        service
          .latestFile("minion.pdf", Some(Path("/bingo/bango")))
          .futureValue
          .value
      res.filename mustBe "minion.pdf"
      res.metadata.path.get.value mustBe Path("/root/bingo/bango/").value
    }

    "not be possible to upload new version of a file if it isn't locked" in {
      val folder = Path("/root/bingo/")
      val fn     = "minion.pdf"
      val fw     = file(owner, usrId, fn, folder)

      // Save the first version
      val mfid = service.saveFile(fw).futureValue
      mfid.foreach(fileIds += _)
      mfid.success mustBe true
      // Save the second version
      service.saveFile(fw).futureValue mustBe NotLocked()

      val res2 =
        service.latestFile(fn, Some(folder)).futureValue.value
      res2.filename mustBe fn
      res2.metadata.path.get.value mustBe folder.value
      res2.metadata.version mustBe 1
    }

    "not be possible moving a file to folder having file with same name" in {
      val orig = Path("/root/foo/bar")
      val dest = Path("/root/bingo/bango")
      val fn   = "minion.pdf"
      val fw   = file(owner, usrId, fn, orig)

      val mfid = service.saveFile(fw).futureValue
      mfid.foreach(fileIds += _)
      mfid.success mustBe true

      service.moveFile(fn, orig, dest).futureValue mustBe an[IllegalDestination]
    }

    "do nothing when attempting to move a file that doesn't exist" in {
      val orig = Path("/root/foo/bar")
      val dest = Path("/root/bingo/bango")

      service
        .moveFile(FileId(UUID.randomUUID().toString), orig, dest)
        .futureValue mustBe NotFound()
    }

    "be possible to add new version of file if locked by the same user" in {
      val folder = Path("/root/bingo/")
      val fn     = "locked-with-version.pdf"
      val fw     = file(owner, usrId, fn, folder)
      // Save the first version
      val mf1 = service.saveFile(fw).futureValue
      mf1.foreach(fileIds += _)
      mf1.success mustBe true

      // Lock the file
      val lockRes = service.lockFile(mf1.get).futureValue
      lockRes.success mustBe true

      // Save the second version
      val mf2 = service.saveFile(fw).futureValue
      mf2.foreach(fileIds += _)
      mf2.success mustBe true

      val res2 = service.latestFile(fn, Some(folder)).futureValue.value
      res2.filename mustBe fn
      res2.metadata.path.get.value mustBe folder.value
      res2.metadata.version mustBe 2
      res2.metadata.lock mustBe lockRes.toOption
    }

    "be possible to add a new file without a FileStream" in {
      val folder     = Path("/root/bingo/")
      val fn         = "file-without-filestream.pdf"
      val fw         = file(owner, usrId, fn, folder)
      val fwNoStream = fw.copy(stream = None)
      // Save the first version
      val mf1 = service.saveFile(fwNoStream).futureValue
      mf1.foreach(fileIds += _)
      mf1.success mustBe true

      val res1 = service.latestFile(fn, Some(folder)).futureValue.value
      res1.filename mustBe fn
      res1.stream mustBe empty
      res1.metadata.path.get.value mustBe folder.value
      res1.metadata.version mustBe 1
      res1.metadata.lock mustBe empty
    }

    "add new version with a FilesStream to a file prev without FileStream" in {
      val folder     = Path("/root/bingo/")
      val fn         = "file-without-fs-2.pdf"
      val fw         = file(owner, usrId, fn, folder)
      val fwNoStream = fw.copy(stream = None)

      // Save the first version
      val mf1 = service.saveFile(fwNoStream).futureValue
      mf1.foreach(fileIds += _)
      mf1.success mustBe true

      val res1 = service.latestFile(fn, Some(folder)).futureValue.value
      res1.filename mustBe fn
      res1.stream mustBe empty
      res1.metadata.path.get.value mustBe folder.value
      res1.metadata.version mustBe 1
      res1.metadata.lock mustBe empty

      // Lock the file
      val lockRes = service.lockFile(mf1.get).futureValue
      lockRes.success mustBe true

      // Save the second version
      val mf2 = service.saveFile(fw).futureValue
      mf2.foreach(fileIds += _)
      mf2.success mustBe true

      val res2 = service.latestFile(fn, Some(folder)).futureValue.value
      res2.filename mustBe fn
      res2.metadata.path.get.value mustBe folder.value
      res2.metadata.version mustBe 2
      res2.metadata.lock mustBe lockRes.toOption
      res2.stream must not be empty
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
      mf1.success mustBe true

      // Lock the file
      val lockRes = service.lockFile(mf1.get).futureValue
      lockRes.success mustBe true

      // Attempt to save the second version as another user

      service.saveFile(fw)(localCtx, global).futureValue match {
        case ResourceLocked(msg, by) =>
          by mustBe Some(usrId)

        case err =>
          fail(s"Expected a ResourceLocked by got ${err.getClass}")
      }

      val res2 = service.latestFile(fn, Some(folder)).futureValue.value
      res2.filename mustBe fn
      res2.metadata.path.get.value mustBe folder.value
      res2.metadata.version mustBe 1
      res2.metadata.lock mustBe lockRes.toOption
    }

    "be possible to lookup all versions of file by the name and path" in {
      val folder = Path("/root/bingo/bango/")
      val fn     = "multiversion.pdf"
      val fw     = file(owner, usrId, fn, folder)

      // Save a few versions of the document
      val v1 = service.saveFile(fw).futureValue
      v1.foreach(fileIds += _)
      service.lockFile(v1.get).futureValue.success mustBe true
      for (x <- 1 to 4) {
        service.saveFile(fw).futureValue.success mustBe true
      }

      val res = service.listFiles(fn, Some(folder)).futureValue.value
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

      val original = service.listFiles(fn, Some(from)).futureValue.value

      val res = service.moveFile(fn, from, to).futureValue.value
      res.filename mustBe fn
      res.metadata.path must not be empty
      res.metadata.path.get.materialize mustBe to.materialize

      service
        .listFiles(fn, Some(to))
        .futureValue
        .value
        .size mustBe original.size
    }

    // Basically preparing for folder locking tests
    "place a new lock on a specific file" in {
      val fid = getFileId(8) // is a deep child of folder with index 1
      service.lockFile(fid).futureValue.success mustBe true
    }

    "be possible to lock a folder and its entire sub-tree" in {
      val fidToLock = getFolderId(1)

      val res = service.lockFolder(fidToLock).futureValue
      res.success mustBe true

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

      service.moveFile(fid, orig, dest).futureValue mustBe an[ResourceLocked]

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
      service.saveFile(fw).futureValue mustBe a[ResourceLocked]
    }

    "prevent adding a lock on a file if folder tree is locked" in {
      val fid = getFileId(4)
      service.lockFile(fid).futureValue mustBe a[ResourceLocked]
    }

    "prevent moving a folder in the sub-tree of a locked folder" in {
      val fid    = getFolderId(2)
      val before = service.folder(fid).futureValue.value
      val orig   = before.flattenPath
      val dest   = Path("/bingo/bango/")

      service.moveFolder(orig, dest).futureValue mustBe NotModified()

      val after = service.folder(fid).futureValue.value

      after.flattenPath mustBe before.flattenPath
    }

    "be possible to unlock a folder and its entire sub-tree" in {
      val fid = getFolderId(1)
      service.unlockFolder(fid).futureValue mustBe Ok(())
    }

    "still return the full sub-tree" in {
      service.treeWithFiles(Some(Path("/hoo"))).futureValue.value.size mustBe 5

      service
        .treeWithFiles(Some(Path("/bingo/bango")))
        .futureValue
        .value
        .size mustBe 5
    }

    "prevent removing a folder with files in its sub-tree" in {
      val fid = getFolderId(1)

      service.deleteFolder(fid).futureValue mustBe a[ResourceLocked]
    }

    "prevent removing a folder with a locked folder in its sub-tree" in {
      val p          = Path("/yes/no/maybe/something/else")
      val lockedPath = p.parent

      // Prepare clean data set
      service.createFolder(p).futureValue.success mustBe true

      val del = service.folder(Path("/yes")).futureValue.value.metadata.fid
      val lkd = service.folder(lockedPath).futureValue.value.metadata.fid

      service.lockFolder(lkd.value).futureValue.success mustBe true
      service.deleteFolder(del.value).futureValue mustBe a[ResourceLocked]
    }

    "prevent removing a folder in a locked sub-tree" in {
      val p = Path("/yes/no/maybe/something")

      val del = service.folder(p).futureValue.value.metadata.fid
      service.deleteFolder(del.value).futureValue mustBe a[ResourceLocked]
    }

    "allow removing a folder with no files or locked folders in sub-tree" in {
      val p       = Path("/abc/def/ghi/jkl/mno")
      val delPath = Path("/abc/def")

      service.createFolder(p).futureValue.success mustBe true

      val del = service.folder(delPath).futureValue.value.metadata.fid

      service.deleteFolder(del.value).futureValue.success mustBe true

      service.folder(del.value).futureValue mustBe NotFound()
    }

    "prevent removing a file locked by a different user" in {
      val p  = Some(Path("/foo/bar"))
      val fn = "lock-me.pdf"

      val currCtx = ctx.copy(currentUser = usrId2)

      val id = service.latestFile(fn, p).futureValue.value.metadata.fid

      service.deleteFile(id.value)(currCtx, global).futureValue match {
        case ResourceLocked(msg, mp) =>
          mp mustBe Some(usrId)

        case err =>
          fail(s"Expected ResourceLocked but got ${err.getClass}")
      }
    }

    "allow removing a file that is locked by current user" in {
      val p  = Some(Path("/foo/bar"))
      val fn = "lock-me.pdf"

      val id = service.latestFile(fn, p).futureValue.value.metadata.fid

      service.deleteFile(id.value).futureValue.success mustBe true

      service.file(id.value).futureValue mustBe NotFound()
    }

    "allow removing a file without lock" in {
      val p  = Some(Path("/foo"))
      val fn = "unlock-me.pdf"

      val id = service.latestFile(fn, p).futureValue.value.metadata.fid

      service.file(id.value).futureValue.value.metadata.lock mustBe empty

      service.deleteFile(id.value).futureValue.success mustBe true

      service.file(id.value).futureValue mustBe NotFound()
    }

    "allow erasing a file and all its versions completely from the system" in {
      val fw = file(owner, usrId, "eraseme.pdf", Path("/yes/no"))
      val fw2 =
        (fid: FileId) => fw.copy(metadata = fw.metadata.copy(fid = Some(fid)))

      val fid = service.saveFile(fw).futureValue.value

      service.lockFile(fid).futureValue.success mustBe true
      service.saveFile(fw2(fid)).futureValue mustBe Ok(fid)
      service.saveFile(fw2(fid)).futureValue mustBe Ok(fid)
      service.unlockFile(fid).futureValue mustBe Ok(())

      service.file(fid).futureValue.value.metadata.version mustBe 3

      service.eraseFile(fid).futureValue.success mustBe true
    }

    "be possible to move a folder and all its contents" in {
      val orig = Path("/root/foo")
      val dest = Path("/root/red/blue/foo")

      val origTree = service.treeWithFiles(Some(dest.parent)).futureValue.value
      val moveTree = service.treeWithFiles(Some(orig)).futureValue.value

      val res1 = service.moveFolder(orig, dest).futureValue.value
      res1.size mustBe 2
      res1 must contain only (dest, dest.append("bar"))

      val destTree1 = service.treeWithFiles(Some(dest.parent)).futureValue.value
      destTree1.size mustBe origTree.size + moveTree.size

      // move back
      val res2 = service.moveFolder(dest, orig).futureValue.value
      res2.size mustBe 2
      res2 must contain only (orig, orig.append("bar"))

      val destTree2 = service.treeWithFiles(Some(dest.parent)).futureValue.value
      destTree2 mustBe origTree
    }

  }

}
