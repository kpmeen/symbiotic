package models.party

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{FileStream, SymbioticDocument}
import org.joda.time.DateTime

case class Avatar(
    id: Option[UUID] = None,
    uploadDate: Option[DateTime] = None,
    length: Option[String] = None,
    filename: String,
    fileType: Option[String] = None,
    stream: Option[FileStream] = None,
    metadata: AvatarMetadata
) extends SymbioticDocument[AvatarMetadata]

object Avatar {

  def apply(
      uid: UserId,
      ctype: Option[String],
      s: Option[FileStream]
  ): Avatar =
    Avatar(
      filename = uid.value,
      fileType = ctype,
      stream = s,
      metadata = AvatarMetadata(uid)
    )

}
