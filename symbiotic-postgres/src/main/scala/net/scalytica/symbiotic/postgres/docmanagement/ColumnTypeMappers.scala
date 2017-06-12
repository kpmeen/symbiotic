package net.scalytica.symbiotic.postgres.docmanagement

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{FileId, Path}
import net.scalytica.symbiotic.postgres.SymbioticDb

trait ColumnTypeMappers { self: SymbioticDb =>

  import profile.api._

  implicit val fileIdMapper: BaseColumnType[FileId] =
    MappedColumnType.base[FileId, UUID](FileId.unsafeAsUuid, FileId.fromUuid)

  implicit val pathMapper: BaseColumnType[Path] =
    MappedColumnType.base[Path, String](_.materialize, Path.apply)

  implicit val userIdMapper: BaseColumnType[UserId] =
    MappedColumnType.base[UserId, UUID](UserId.unsafeAsUuid, UserId.fromUuid)

  implicit val dateTimeMapper: BaseColumnType[org.joda.time.DateTime] =
    MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
      dt => new java.sql.Timestamp(dt.getMillis),
      ts => new org.joda.time.DateTime(ts)
    )

}
