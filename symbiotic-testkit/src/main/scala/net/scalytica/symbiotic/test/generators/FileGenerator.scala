package net.scalytica.symbiotic.test.generators

import akka.stream.IOResult
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime

import scala.concurrent.Future

object FileGenerator {

  def maybeFileStream: Option[Source[ByteString, Future[IOResult]]] = Option(
    StreamConverters.fromInputStream(
      () => this.getClass.getResourceAsStream("/files/test.pdf")
    )
  )

  val extraAttributes = MetadataMap(
    "customString" -> "some string",
    "customInt"    -> "some int",
    "customBool"   -> false,
    "customDouble" -> 333.33d,
    "customLong"   -> Long.MaxValue,
    "customDate"   -> DateTime.now().withTimeAtStartOfDay().minusYears(2)
  )

  def file(
      owner: Owner,
      by: UserId,
      fname: String,
      folder: Path,
      fileId: Option[FileId] = FileId.createOpt(),
      version: Int = 1
  ): File =
    File(
      filename = fname,
      fileType = Some("application/pdf"),
      createdDate = Some(DateTime.now),
      stream = maybeFileStream,
      metadata = ManagedMetadata(
        owner = Some(owner),
        accessibleBy = Seq(AllowedParty(owner.id)),
        fid = fileId,
        isFolder = false,
        createdBy = Some(by),
        version = version,
        path = Some(folder),
        description = Some("This is a test"),
        extraAttributes = Some(extraAttributes)
      )
    )

  def files(
      owner: Owner,
      by: UserId,
      folders: Seq[Folder]
  ): Seq[File] =
    folders.grouped(3).map(_.lastOption).flatten.zipWithIndex.toSeq.flatMap {
      case (f, i) =>
        val fid = FileId.createOpt()
        Seq(
          file(owner, by, s"file-$i", f.flattenPath, fid),
          file(owner, by, s"file-$i", f.flattenPath, fid, 2)
        )
    }

}
