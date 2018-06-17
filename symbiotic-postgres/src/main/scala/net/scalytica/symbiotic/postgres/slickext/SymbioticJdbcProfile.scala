package net.scalytica.symbiotic.postgres.slickext

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes._
import net.scalytica.symbiotic.api.types.ResourceParties._
import net.scalytica.symbiotic.api.types.{FileId, Path}
import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.utils.SimpleArrayUtils
import play.api.libs.json.{JsValue, Json}
import slick.ast.Library.SqlOperator
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

object ExtOperators {
  val ~ = new SqlOperator("~")
}

trait SymbioticJdbcProfile
    extends ExPostgresProfile
    with PgArraySupport
    with PgDate2Support
    with PgRangeSupport
    with PgHStoreSupport
    with PgPlayJsonSupport
    with PgSearchSupport
    with PgNetSupport
    with PgLTreeSupport
    with PgEnumSupport {

  override def pgjson = "jsonb"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert`
  // support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  trait ExtraApi
      extends API
      with ArrayImplicits
      with JsonImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {

    implicit def pathColumn(
        c: Rep[Path]
    ): PathExtensionMethods[Path] =
      new PathExtensionMethods[Path](c)

    implicit def pathOptColumn(
        c: Rep[Option[Path]]
    ): PathExtensionMethods[Option[Path]] =
      new PathExtensionMethods[Option[Path]](c)

    implicit val strListTypeMapper: DriverJdbcType[List[String]] =
      new SimpleArrayJdbcType[String]("text").to(_.toList)

    implicit val playJsonArrayTypeMapper: DriverJdbcType[List[JsValue]] =
      new AdvancedArrayJdbcType[JsValue](
        pgjson,
        s => SimpleArrayUtils.fromString[JsValue](Json.parse)(s).orNull,
        v => SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)

    implicit val fileIdMapper: BaseColumnType[FileId] =
      MappedColumnType.base[FileId, UUID](FileId.unsafeAsUuid, FileId.fromUuid)

    implicit val pathMapper: BaseColumnType[Path] =
      MappedColumnType.base[Path, String](_.materialize, Path.apply)

    implicit val userIdMapper: BaseColumnType[UserId] =
      MappedColumnType.base[UserId, UUID](UserId.unsafeAsUuid, UserId.fromUuid)

    implicit val ownerTypeMapper: BaseColumnType[Type] =
      MappedColumnType.base[Type, String](_.tpe, Type.fromString)

    implicit val dateTimeMapper: BaseColumnType[org.joda.time.DateTime] =
      MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
        dt => new java.sql.Timestamp(dt.getMillis),
        ts => new org.joda.time.DateTime(ts)
      )
  }

  override val api: ExtraApi = new ExtraApi {}

}

object SymbioticJdbcProfile extends SymbioticJdbcProfile
