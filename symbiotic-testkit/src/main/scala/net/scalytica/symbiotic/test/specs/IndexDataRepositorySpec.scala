package net.scalytica.symbiotic.test.specs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import net.scalytica.symbiotic.api.repository.{
  FileRepository,
  FolderRepository,
  IndexDataRepository
}
import net.scalytica.symbiotic.api.types.ResourceParties.{Org, Owner}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.test.generators.{
  FileGenerator,
  FolderGenerator,
  TestContext,
  TestUserId
}
import org.scalatest.Inspectors.forAll
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpecLike}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class IndexDataRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with PersistenceSpec {

  private val logger =
    LoggerFactory.getLogger(classOf[IndexDataRepositorySpec])

  // scalastyle:off magic.number
  val usrId   = TestUserId.create()
  val ownerId = usrId
  val owner   = Owner(ownerId, Org)

  implicit val ctx: TestContext = TestContext(usrId, owner, Seq(owner.id))

  implicit val actorSystem: ActorSystem        = ActorSystem("file-repo-test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val fileRepo: FileRepository
  val folderRepo: FolderRepository
  val indexProvider: IndexDataRepository

  val folders = {
    Seq(Folder(ownerId, Path.root)) ++ FolderGenerator.createFolders(
      owner = ownerId,
      baseName = "folder_a",
      depth = 3
    ) ++ FolderGenerator.createFolders(
      owner = ownerId,
      baseName = "folder_b",
      depth = 5
    ) ++ FolderGenerator.createFolders(
      owner = ownerId,
      baseName = "folder_c",
      depth = 2
    )
  }

  val files = FileGenerator.files(owner, usrId, folders)

  override def beforeAll() = {
    super.beforeAll()
    val a = folders.flatMap { f =>
      Await.result(folderRepo.save(f), 5 seconds).toOption
    }
    val b = files.flatMap { f =>
      Await.result(fileRepo.save(f), 5 seconds).toOption
    }
    logger.debug(s"Pre-loaded ${a.size} folders and ${b.size} files.")
  }

  override def afterAll() = {
    materializer.shutdown()
    actorSystem.terminate()
    super.afterAll()
  }

  val testFold: Sink[ManagedFile, Future[(Seq[Folder], Seq[File])]] =
    Sink.fold((Seq.empty[Folder], Seq.empty[File])) {
      case (mfs, f: Folder) => (f +: mfs._1) -> mfs._2
      case (mfs, f: File)   => mfs._1 -> (f +: mfs._2)
      case (mfs, _)         => mfs
    }

  def runTestFold(
      src: Source[ManagedFile, NotUsed]
  ): Future[(Seq[Folder], Seq[File])] = src.runWith(testFold)

  "The IndexDataProvider" should {
    "provide a stream of all managed files and their latest version" in {
      val n = runTestFold(indexProvider.streamAll()).futureValue

      n._1.size mustBe 11
      n._2.size mustBe 4 // There are 4 unique files, each with 2 versions.

      forAll(n._1)(_.metadata.isFolder mustBe true)
      forAll(n._2) { file =>
        file.metadata.isFolder mustBe false
        file.metadata.version mustBe 2
      }
    }

  }
}
