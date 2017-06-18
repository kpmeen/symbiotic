package net.scalytica.symbiotic.fs

import java.io.{File => JFile}
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.api.types.{File, FileId, ManagedFileMetadata}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

import scala.collection.JavaConverters._

class FileSystemIOSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with BeforeAndAfterAll {

  val timeout: Double  = 10000d
  val interval: Double = 15d

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(timeout, Millis),
    interval = Span(interval, Millis)
  )

  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val testRootDir = "target/dman_root"
  val cm          = Map("symbiotic.fs.rootDir" -> testRootDir).asJava
  val config      = ConfigFactory.parseMap(cm)

  val fileUri = classOf[FileSystemIO].getClassLoader
    .getResource("test_files/clean.pdf")
    .toURI
  val filePath = new JFile(fileUri).toPath

  val fs         = new FileSystemIO(config)
  val fileSource = FileIO.fromPath(filePath)

  val symbioticFile = File(
    id = Some(UUID.randomUUID()),
    filename = "clean.pdf",
    contentType = Some("application/pdf"),
    uploadDate = Some(DateTime.now()),
    length = None,
    stream = Some(fileSource),
    metadata = ManagedFileMetadata(fid = FileId.createOpt())
  )

  override def afterAll() = {
    materializer.shutdown()
    system.terminate()
  }

  "The FileSystemIO class" should {
    "successfully write a file to the persistent file store" in {
      fs.write(symbioticFile).futureValue.isRight mustBe true
    }

    "successfully read a file from the persistent file store" in {
      val res = fs.read(symbioticFile.copy(stream = None))
      res must not be empty
    }
  }

}
