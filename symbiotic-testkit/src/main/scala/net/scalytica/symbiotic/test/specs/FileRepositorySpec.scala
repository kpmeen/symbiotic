package net.scalytica.symbiotic.test.specs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.scalytica.symbiotic.api.SymbioticResults.{NotFound, Ok}
import net.scalytica.symbiotic.api.repository.{FileRepository, FolderRepository}
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.{
  JodaValue,
  MetadataMap
}
import net.scalytica.symbiotic.api.types.ResourceParties.{
  AllowedParty,
  Org,
  Owner
}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.test.generators.FileGenerator.file
import net.scalytica.symbiotic.test.generators._
import net.scalytica.symbiotic.test.utils.SymResValues
import net.scalytica.symbiotic.time.SymbioticDateTime._
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
    with SymResValues
    with PersistenceSpec {

  // scalastyle:off magic.number
  val usrId1  = TestUserId.create()
  val orgId1  = TestOrgId.create()
  val ownerId = usrId1
  val owner   = Owner(ownerId, Org)

  val usrId2 = TestUserId.create()
  val orgId2 = TestOrgId.create()

  val accessors = Seq(AllowedParty(usrId2), AllowedParty(orgId2))

  implicit val ctx: TestContext = TestContext(usrId1, owner, Seq(owner.id))
  val ctx2                      = TestContext(usrId2, owner, Seq(usrId2))

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

  override def beforeAll(): Unit = {
    super.beforeAll()
    folders.flatMap { f =>
      val r = Await.result(folderRepo.save(f), 5 seconds)
      r.toOption
    }.foreach(res => folderIds += res)
  }

  override def afterAll(): Unit = {
    materializer.shutdown()
    actorSystem.terminate()
    super.afterAll()
  }

  "The file repository" should {

    "successfully save a file" in {
      val f   = file(owner, usrId1, "file1", folders(2).flattenPath)
      val res = fileRepo.save(f).futureValue
      res.map(fileIds += _)

      res.success mustBe true
    }

    "save another file" in {
      val f   = file(owner, usrId1, "file2", folders(2).flattenPath)
      val res = fileRepo.save(f).futureValue
      res.map(fileIds += _)

      res.success mustBe true
    }

    "not be able to save a file in a folder without access" in {
      val f = file(owner, usrId1, "file3", folders(2).flattenPath)
      fileRepo.save(f)(ctx2, global).futureValue.failed mustBe true
    }

    "find the file with a specific name and path" in {
      val path = Some(folders(2).flattenPath)
      val res  = fileRepo.find("file2", path).futureValue.value

      res.size mustBe 1
      res.headOption.value.filename mustBe "file2"
      res.headOption.value.fileType mustBe Some("application/pdf")
      res.headOption.value.metadata.path mustBe path
      res.headOption.value.metadata.version mustBe 1
      res.headOption.value.metadata.extraAttributes must not be empty
      val ea      = res.headOption.value.metadata.extraAttributes.value
      val expAttr = FileGenerator.extraAttributes.toSeq
      forAll(expAttr) {
        case (k: String, v: JodaValue) =>
          ea.get(k).map(_.toString) mustBe Some(v.toString)

        case kv =>
          ea.get(kv._1) mustBe Some(kv._2)
      }
    }

    "not return a file with a specific name and path without access" in {
      val path = Some(folders(2).flattenPath)
      fileRepo.find("file2", path)(ctx2, global).futureValue mustBe NotFound()
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

      val res = fileRepo.save(f).futureValue.value

      res mustBe fileIds.result().headOption.value
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

      fileRepo.save(f)(ctx2, global).futureValue.failed mustBe true
    }

    "find the latest version of a file" in {
      val path = Some(folders(2).flattenPath)
      val res  = fileRepo.findLatest("file1", path).futureValue.value

      res.filename mustBe "file1"
      res.fileType mustBe Some("application/pdf")
      res.metadata.path mustBe path
      res.metadata.version mustBe 2
    }

    "update the metadata on the latest version of a file" in {
      val expDesc     = "Updated metadata"
      val expExtAttrs = MetadataMap("addedKey" -> "set by update")
      val fid         = fileIds.result().headOption.value
      val orig        = fileRepo.findLatestBy(fid).futureValue.value

      val mod = orig.copy(
        metadata = orig.metadata.copy(
          description = Some(expDesc),
          extraAttributes = orig.metadata.extraAttributes.map(_ ++ expExtAttrs)
        )
      )

      val res = fileRepo.updateMetadata(mod).futureValue.value

      res.id mustBe orig.id
      res.filename mustBe orig.filename
      res.metadata.version mustBe orig.metadata.version
      res.metadata.description mustBe Some(expDesc)
      res.metadata.extraAttributes.value.toSeq must contain(
        expExtAttrs.headOption.value
      )
    }

    "not return the latest version of a file without access" in {
      val path = Some(folders(2).flattenPath)
      fileRepo
        .findLatest("file1", path)(ctx2, global)
        .futureValue
        .failed mustBe true
    }

    "list all files at a given path" in {
      val fseq = Seq(
        file(owner, usrId1, "file3", folders(1).flattenPath),
        file(owner, usrId1, "file4", folders(3).flattenPath),
        file(owner, usrId1, "file5", folders(2).flattenPath)
      )
      // Add the files
      fseq.foreach { f =>
        fileIds += fileRepo.save(f).futureValue.value
      }

      val res = fileRepo.listFiles(folders(2).flattenPath).futureValue.value

      res.size mustBe 3
      res.map(_.filename) must contain only ("file1", "file2", "file5")
      res.find(_.filename == "file1").value.metadata.version mustBe 2
      res.find(_.filename == "file2").value.metadata.version mustBe 1
      res.find(_.filename == "file5").value.metadata.version mustBe 1
    }

    "not list all files at a given path without access" in {
      fileRepo
        .listFiles(folders(2).flattenPath)(ctx2, global)
        .futureValue
        .value mustBe empty
    }

    "not lock a file without access" in {
      val fid = fileIds.result()(4)
      fileRepo.lock(fid)(ctx2, global).futureValue mustBe NotFound()
    }

    "lock a file" in {
      val fid = fileIds.result()(4)
      fileRepo.lock(fid).futureValue match {
        case Ok(lock) =>
          lock.by mustBe usrId1
          lock.date.getDayOfYear mustBe now.getDayOfYear

        case wrong =>
          fail(s"Expected LockApplied[Option[Lock]], got ${wrong.getClass}")
      }
    }

    "return the user id of the lock owner on a locked file" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid).futureValue mustBe Ok(Some(usrId1))
    }

    "not return any information about a locked file without access" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid)(ctx2, global).futureValue mustBe NotFound()
    }

    "not unlock a file without access" in {
      val fid = fileIds.result()(4)
      fileRepo.unlock(fid)(ctx2, global).futureValue mustBe NotFound()
    }

    "unlock a file" in {
      val fid = fileIds.result()(4)
      fileRepo.unlock(fid).futureValue mustBe Ok(())
    }

    "return None if the file isn't locked" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid).futureValue.value mustBe None
    }

    "not be allowed to move a file without access" in {
      val origPath = folders(1).flattenPath
      val destPath = folders(3).flattenPath

      fileRepo
        .move("file3", origPath, destPath)(ctx2, global)
        .futureValue mustBe NotFound()
    }

    "move a file" in {
      val origPath = folders(1).flattenPath
      val destPath = folders(3).flattenPath

      fileRepo.listFiles(destPath).futureValue.value.size mustBe 1

      val moved = fileRepo.move("file3", origPath, destPath).futureValue.value
      moved.metadata.path.value mustBe destPath
      moved.filename mustBe "file3"

      fileRepo.listFiles(destPath).futureValue.value.size mustBe 2
    }

    "successfully mark a file as deleted" in {
      val fid = fileIds.result()(4)

      fileRepo.markAsDeleted(fid).futureValue mustBe Ok(1)
    }

    "not return information about a deleted file" in {
      val fid = fileIds.result()(4)

      fileRepo.findLatestBy(fid).futureValue mustBe NotFound()
    }

    "entirely erase all versions of metadata and files for a given File" in {
      val fid = fileIds.result().headOption.value

      // Erasing the file should result in 2 removed versions
      fileRepo.eraseFile(fid).futureValue mustBe Ok(2)

      fileRepo.findLatestBy(fid).futureValue mustBe NotFound()
    }

  }

}
