package net.scalytica.symbiotic.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import net.scalytica.symbiotic.api.repository.{FileRepository, FolderRepository, IndexDataRepository}
import net.scalytica.symbiotic.api.types.ResourceParties.{Org, Owner}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.test.generators.{FileGenerator, FolderGenerator, TestContext, TestUserId}
import net.scalytica.symbiotic.test.specs.ElasticSearchSpec
import net.scalytica.symbiotic.test.utils.DelayedExecution.delay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

abstract class ElasticSearchIndexerSpec extends ElasticSearchSpec { self =>

  // scalastyle:off magic.number
  val usrId   = TestUserId.create()
  val ownerId = usrId
  val owner   = Owner(ownerId, Org)

  val actorSystemName: String = self.getClass.getSimpleName.toLowerCase

  // scalastyle:off line.size.limit
  implicit val ctx: TestContext       = TestContext(usrId, owner, Seq(owner.id))
  implicit val sys: ActorSystem       = ActorSystem(s"$actorSystemName-indexer-test")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  // scalastyle:on

  val fileRepo: FileRepository
  val folderRepo: FolderRepository
  val indexDataRepo: IndexDataRepository

  lazy val indexer = ElasticSearchIndexer(config, RefreshPolicy.IMMEDIATE)

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
    ElasticSearchIndexer.removeIndicies(config)
    Await
      .result(
        Future.sequence(folders.map(f => folderRepo.save(f))),
        5 seconds
      )
      .flatMap(_.toOption)
      .foreach(folderIds += _)
  }

  override def afterAll(): Unit = {
    ElasticSearchIndexer.removeIndicies(config)
    mat.shutdown()
    sys.terminate()
    super.afterAll()
  }

  "The ElasticSearchIndexer" should {

    "send a Folder for indexing" in {
      val f = FolderGenerator.createFolder(
        owner = ownerId,
        name = "single folder"
      )

      indexer.index(f).futureValue mustBe true
    }

    "send a File for indexing" in {
      val f = FileGenerator.file(
        owner = owner,
        by = ownerId,
        fname = "singlefile.pdf",
        folder = Path.root
      )

      indexer.index(f).futureValue mustBe true
    }

    "send a stream of ManagedFiles for indexing" in {
      val esc = new ElasticSearchClient(config)
      // Ensure we have a clean index
      ElasticSearchIndexer.removeIndicies(config)
      ElasticSearchIndexer.initIndices(config)

      def src: Source[ManagedFile, NotUsed] = indexDataRepo.streamAll()
      // Verify the source
      src.runFold(0)((a, _) => a + 1).futureValue mustBe 4

      indexer.indexSource(src)

      val res = delay(
        () =>
          esc.exec(searchWithType((config: ElasticSearchConfig).indexAndType)),
        2 seconds
      ).futureValue.right

      res.value.result.hits.total mustEqual 4
    }

  }

}
