package net.scalytica.symbiotic.test.specs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.scalytica.symbiotic.api.persistence.{
  FileRepository,
  FolderRepository
}
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.JodaValue
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes.{
  LockApplied,
  LockRemoved
}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.test.generators.FileGenerator.file
import net.scalytica.symbiotic.test.generators.{
  FileGenerator,
  FolderGenerator,
  TestUserId
}
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

abstract class FileRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with OptionValues
    with BeforeAndAfterAll {

  // scalastyle:off magic.number
  implicit val uid          = TestUserId.create()
  implicit val transform    = (s: String) => TestUserId.asId(s)
  implicit val actorSystem  = ActorSystem("postgres-test")
  implicit val materializer = ActorMaterializer()

  val fileRepo: FileRepository
  val folderRepo: FolderRepository

  val folders = {
    Seq(Folder(uid, Path.root)) ++ FolderGenerator.createFolders(
      owner = uid,
      baseName = "folder",
      depth = 3
    )
  }

  val folderIds = Seq.newBuilder[FolderId]
  val fileIds   = Seq.newBuilder[FileId]

  override def beforeAll() = {
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
      val f   = file(uid, "file1", folders(2).flattenPath)
      val res = fileRepo.save(f).futureValue
      res.map(fileIds += _)

      res must not be empty
    }

    "save another file" in {
      val f   = file(uid, "file2", folders(2).flattenPath)
      val res = fileRepo.save(f).futureValue
      res.map(fileIds += _)

      res must not be empty
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

    "save a new version of a file" in {
      val f = file(
        uid = uid,
        fname = "file1",
        folder = folders(2).flattenPath,
        fileId = fileIds.result().headOption,
        version = 2
      )

      val res = fileRepo.save(f).futureValue

      res must not be empty
      res mustBe fileIds.result().headOption
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

    "list all files at a given path" in {
      val fseq = Seq(
        file(uid, "file3", folders(1).flattenPath),
        file(uid, "file4", folders(3).flattenPath),
        file(uid, "file5", folders(2).flattenPath)
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

    "lock a file" in {
      val fid = fileIds.result()(4)
      fileRepo.lock(fid).futureValue match {
        case LockApplied(maybeLock) =>
          maybeLock must not be empty
          maybeLock.value.by mustBe uid
          maybeLock.value.date.getDayOfYear mustBe DateTime.now.getDayOfYear

        case wrong =>
          fail(
            s"Expected LockApplied[Option[Lock]], but got ${wrong.getClass}"
          )
      }
    }

    "return the user id of the lock owner on a locked file" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid).futureValue mustBe Some(uid)
    }

    "unlock a file" in {
      val fid = fileIds.result()(4)
      fileRepo.unlock(fid).futureValue mustBe a[LockRemoved[_]]
    }

    "return None if the file isn't locked" in {
      val fid = fileIds.result()(4)
      fileRepo.locked(fid).futureValue mustBe None
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
