package net.scalytica.symbiotic.fs

import java.io.{File => JFile}

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.scalytica.symbiotic.api.types.{File, FileStream}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class FileSystemIO(
    config: Config
)(
    implicit actorSystem: ActorSystem,
    materializer: Materializer
) {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val ec = actorSystem.dispatcher

  val baseDirStr = config.as[String](FileSystemIO.RootDirKey)
  val baseDir    = new JFile(baseDirStr)

  if (!baseDir.exists()) {
    logger.info(s"Initializing ${FileSystemIO.RootDirKey}: $baseDir...")
    new JFile(baseDirStr).mkdirs()
  } else {
    logger.info(s"Skipping initialization of ${FileSystemIO.RootDirKey}...")
  }

  logger.info(s"Symbiotic FS init completed.")

  private def err(msg: String): Future[Either[String, Unit]] = {
    logger.error(msg)
    Future.successful(Left(msg))
  }

  private def destPath(dte: DateTime): JFile = {
    new JFile(
      s"$baseDirStr/${dte.getYear}/${dte.getMonthOfYear}/${dte.getDayOfMonth}"
    )
  }

  private def removeFailed(f: JFile): Unit = {
    if (f.exists()) f.delete()
  }

  def write(file: File): Future[Either[String, Unit]] = {
    (for {
      fid <- file.metadata.fid.map(_.value)
      dte <- file.createdDate
    } yield {
      val dest = destPath(dte)
      val jfile = new JFile(
        s"${dest.toPath.toString}/$fid.${file.metadata.version}"
      )

      if (!dest.exists()) dest.mkdirs()

      file.stream.map {
        _.runWith(FileIO.toPath(jfile.toPath)).map { res =>
          res.status match {
            case Success(_) =>
              logger.debug(
                s"Successfully wrote ${file.filename} with $fid to" +
                  s" to folder ${dest.toPath}"
              )
              Right(())

            case Failure(ex) =>
              logger.error(
                s"An error occurred while attempting to write the file" +
                  s" ${file.filename} with $fid to folder ${dest.toPath}",
                ex
              )
              removeFailed(jfile)
              Left(ex.getMessage)
          }
        }.recover {
          case NonFatal(ex) =>
            logger.error("An unexpected error occurred", ex)
            removeFailed(jfile)
            Left(ex.getMessage)

          case fatal =>
            logger.error("An unexpected fatal error occurred", fatal)
            removeFailed(jfile)
            throw fatal // scalastyle:ignore

        }
      }.getOrElse(err(s"File ${file.filename} did not contain a file stream."))
    }).getOrElse(
      err(s"File ${file.filename} did not have a valid FileId or upload date")
    )
  }

  def read(file: File): Option[FileStream] = {
    (for {
      fid <- file.metadata.fid.map(_.value)
      dte <- file.createdDate
    } yield {

      val dest = destPath(dte)
      val filePath = new JFile(
        s"${dest.toPath.toString}/$fid.${file.metadata.version}"
      ).toPath

      FileIO.fromPath(filePath)
    }).orElse {
      logger.warn(
        s"File ${file.filename} did not have a valid FileId or upload date"
      )
      None
    }
  }

}

object FileSystemIO {
  val RootDirKey = "symbiotic.fs.rootDir"
}
