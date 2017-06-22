package net.scalytica.symbiotic.test.generators

import akka.stream.scaladsl.StreamConverters
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{
  File,
  FileId,
  ManagedFileMetadata,
  Path
}
import org.joda.time.DateTime

object FileGenerator {

  def maybeFileStream = Option(
    StreamConverters.fromInputStream(
      () => this.getClass.getResourceAsStream("/files/test.pdf")
    )
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
      metadata = ManagedFileMetadata(
        owner = Some(uid),
        fid = fileId,
        uploadedBy = Some(uid),
        version = version,
        path = Some(folder),
        description = Some("This is a test")
      )
    )

}
