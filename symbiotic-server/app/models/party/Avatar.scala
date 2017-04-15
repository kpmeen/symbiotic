package models.party

import java.io.InputStream
import java.util.UUID

import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data.SymbioticDocument
import org.joda.time.DateTime

case class Avatar(
    id: Option[UUID] = None,
    uploadDate: Option[DateTime] = None,
    length: Option[String] = None,
    filename: String,
    contentType: Option[String] = None,
    stream: Option[InputStream] = None,
    metadata: AvatarMetadata
) extends SymbioticDocument[AvatarMetadata]

object Avatar {

  def apply(
      uid: SymbioticUserId,
      ctype: Option[String],
      s: Option[InputStream]
  ): Avatar =
    Avatar(
      filename = uid.value,
      contentType = ctype,
      stream = s,
      metadata = AvatarMetadata(uid)
    )

}
