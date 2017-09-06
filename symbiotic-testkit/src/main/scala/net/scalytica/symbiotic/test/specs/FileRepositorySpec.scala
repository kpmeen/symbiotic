package net.scalytica.symbiotic.test.specs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.scalytica.symbiotic.api.repository.{FileRepository, FolderRepository}
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.JodaValue
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes.{
  LockApplied,
  LockError,
  LockRemoved
}
import net.scalytica.symbiotic.api.types.ResourceParties.{
  AllowedParty,
  Org,
  Owner
}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.test.generators.FileGenerator.file
import net.scalytica.symbiotic.test.generators._
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

abstract class FileRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with OptionValues
    with PersistenceSpec {

  // scalastyle:off magic.number
  val usrId1  = TestUserId.create()
  val orgId1  = TestOrgId.create()
  val ownerId = usrId1
  val owner   = Owner(ownerId, Org)

  val usrId2 = TestUserId.create()
  val orgId2 = TestOrgId.create()

  val accessors = Seq(AllowedParty(usrId2), AllowedParty(orgId2))

  implicit val ctx = TestContext(usrId1, owner, Seq(owner.id))
  val ctx2         = TestContext(usrId2, owner, Seq(usrId2))

  implicit val actorSystem: ActorSystem        = ActorSystem("file-repo-test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val fileRepo: FileRepository
  val folderRepo: FolderRepository

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  val folders = {
    Seq(Folder(ownerId, Path.root)) ++ FolderGenerator.createFolders(
      owner = ownerId,
      baseName = "folder",
      depth = 3
    )
  }

  val folderIds = Seq.newBuilder[FolderId]
  val fileIds   = Seq.newBuilder[FileId]

  override def beforeAll() = {
    super.beforeAll()
    folders.flatMap { f =>
      Await.result(folderRepo.save(f), 5 seconds)
    }.foreach(res => folderIds += res)
  }

  override def afterAll() = {
    materializer.shutdown()
    actorSystem.terminate()
    super.afterAll()
  }

  "The file repository" should {

    "successfully save a file" in {
      val f   = file(owner, usrId1, "file1", folders(2).flattenPath)
      val res = fileRepo.save(f).futureValue
      res.map(fileIds += _)

      res must not be empty
    }

    "save another file" in {
      val f   = file(owner, usrId1, "file2", folders(2).flattenPath)
      val res = fileRepo.save(f).futureValue
      res.map(fileIds += _)

      res must not be empty
    }

    "not be able to save a file in a folder without access" in {
      val f = file(owner, usrId1, "file3", folders(2).flattenPath)
      fileRepo.save(f)(ctx2, global).futureValue mustBe empty
    }

    "find the file with a specific name and path" in {
      val path = Some(folders(2).flattenPath)
      val res  = fileRepo.find("file2", path).futureValue

      res.size mustBe 1
      res.head.filename mustBe "file2"
      res.head.fileType mustBe Some("application/pdf")
      res.head.metadata.path mustBe path
      res.head.metadata.version mustBe 1
      res.head.metadata.extraAttributes must not be empty
      val ea      = res.head.metadata.extraAttributes.get
      val expAttr = FileGenerator.extraAttributes
      forAll(expAttr) {
        case (key: String, joda: JodaValue) =>
          ea.get(key).map(_.toString) mustBe Some(joda.toString)

        case kv =>
          ea.get(kv._1) mustBe Some(kv._2)
      }
    }

    "not return a file with a specific name and path without access" in {
      val path = Some(folders(2).flattenPath)
      fileRepo.find("file2", path)(ctx2, global).futureValue mustBe empty
    }

    "save a new version of a file" in {
      val f = file(
        owner = owner,
        by = usrId1,
        fname = "file1",
        folder = folders(2).flattenPath,
        fileId = fileIds.result().headOption,
        version = 2
      )

      val res = fileRepo.save(f).futureValue

      res must not be empty
      res mustBe fileIds.result().headOption
    }

    "not allow saving a new version without access" in {
      val f = file(
        owner = owner,
        by = usrId1,
        fname = "file1",
        folder = folders(2).flattenPath,
        fileId = fileIds.result().headOption,
        version = 3
      )

      fileRepo.save(f)(ctx2, global).futureValue mustBe empty
    }

    "find the latest version of a file" in {
      val path = Some(folders(2).flattenPath)
      val res  = fileRepo.findLatest("file1", path).futureValue

      res must not be empty
      res.get.filename mustBe "file1"
      res.get.fileType mustBe Some("application/pdf")
      res.head.metadata.path mustBe path
      res.head.metadata.version mustBe 2
    }

    "not return the latest version of a file without access" in {
      val path = Some(folders(2).flattenPath)
      fileRepo.findLatest("file1", path)(ctx2, global).futureValue mustBe empty
    }

    "list all files at a given path" in {
      val fseq = Seq(
        file(owner, usrId1, "file3", folders(1).flattenPath),
        file(owner, usrId1, "file4", folders(3).flattenPath),
        file(owner, usrId1, "file5", folders(2).flattenPath)
      )
      // Add the files
      fseq.foreach { f =>
        val fid = fileRepo.save(f).futureValue
        fid must not be empty
        fileIds += fid.get
      }

      val res = fileRepo.listFiles(folders(2).flattenPath).futureValue

      res.size mustBe 3
      res.map(_.filename) must contain only ("file1", "file2", "file5")
      res.find(_.filename == "file1").value.metadata.version mustBe 2
      res.find(_.filename == "file2").value.metadata.version mustBe 1
      res.find(_.filename == "file5").value.metadata.version mustBe 1
    }

    "not list all files at a given path without access" in {
      fileRepo
        .listFiles(folders(2).flattenPath)(ctx2, global)
        .futureValue mustBe empty
    }

    "not lock a file without access" in {
      val fid = fileIds.result()(4)
      fileRepo.lock(fid)(ctx2, global).futureValue mustBe a[LockError]
    }

    "lock a file" in {
      val fid = fileIds.result()(4)
      fileRepo.lock(fid).futureValue match {
        case LockApplied(maybeLock) =>
          maybeLock must not be empty
          maybeLock.value.by mustBe usrId1
          maybeLock.value.date.getDayOfYear mustBe DateTime.now.getDayOfYear

        case wrong =>
          fail(s"Expected LockApplied[Option[Lock]], got ${wrong.getClass}")
      }
    }

    "return the user id of the lock owner on a locked file" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid).futureValue mustBe Some(usrId1)
    }

    "not return any information about a locked file without access" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid)(ctx2, global).futureValue mustBe empty
    }

    "not unlock a file without access" in {
      val fid = fileIds.result()(4)
      fileRepo.unlock(fid)(ctx2, global).futureValue mustBe a[LockError]
    }

    "unlock a file" in {
      val fid = fileIds.result()(4)
      fileRepo.unlock(fid).futureValue mustBe a[LockRemoved[_]]
    }

    "return None if the file isn't locked" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid).futureValue mustBe None
    }

    "not be allowed to move a file without access" in {
      val origPath = folders(1).flattenPath
      val destPath = folders(3).flattenPath

      fileRepo
        .move("file3", origPath, destPath)(ctx2, global)
        .futureValue mustBe empty
    }

    "move a file" in {
      val origPath = folders(1).flattenPath
      val destPath = folders(3).flattenPath

      fileRepo.listFiles(destPath).futureValue.size mustBe 1

      val moved = fileRepo.move("file3", origPath, destPath).futureValue.value
      moved.metadata.path.value mustBe destPath
      moved.filename mustBe "file3"

      fileRepo.listFiles(destPath).futureValue.size mustBe 2
    }

  }

}
