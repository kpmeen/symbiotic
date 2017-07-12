package net.scalytica.symbiotic.test.generators

import akka.stream.scaladsl.StreamConverters
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{File, FileId, ManagedMetadata, Path}
import org.joda.time.DateTime

object FileGenerator {

  def maybeFileStream = Option(
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
      uid: UserId,
      fname: String,
      folder: Path,
      fileId: Option[FileId] = FileId.createOpt(),
      version: Int = 1
  ) =
    File(
      filename = fname,
      contentType = Some("application/pdf"),
      uploadDate = Some(DateTime.now),
      stream = maybeFileStream,
      metadata = ManagedMetadata(
        owner = Some(uid),
        fid = fileId,
        uploadedBy = Some(uid),
        version = version,
        path = Some(folder),
        description = Some("This is a test"),
        extraAttributes = Some(extraAttributes)
      )
    )

}
