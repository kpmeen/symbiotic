package repository.postgres

import java.util.UUID

import models.base._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{PartyId, UserId}
import net.scalytica.symbiotic.postgres.SymbioticDb

trait ExtraColumnMappers {
  self: SymbioticDb =>

  import profile.api._

  implicit val symbioticUserIdMapper: BaseColumnType[SymbioticUserId] =
    MappedColumnType.base[SymbioticUserId, UUID](
      SymbioticUserId.unsafeAsUuid,
      SymbioticUserId.fromUuid
    )

  implicit val usernameMapper: BaseColumnType[Username] =
    MappedColumnType.base[Username, String](_.value, Username.apply)

  implicit val emailMapper: BaseColumnType[Email] =
    MappedColumnType.base[Email, String](_.adr, Email.apply)

  implicit val genderTypeMapper = profile.createEnumJdbcType[Gender](
    sqlEnumTypeName = "gender_type",
    enumToString = _.strValue,
    stringToEnum = Gender.unsafeFromString,
    quoteName = false
  )
  implicit val genderTypeListMapper = profile.createEnumListJdbcType[Gender](
    sqlEnumTypeName = "gender_type",
    enumToString = _.strValue,
    stringToEnum = Gender.unsafeFromString,
    quoteName = false
  )

  implicit val genderColumnExtensionMethodsBuilder =
    profile.createEnumColumnExtensionMethodsBuilder[Gender]

  implicit val genderOptionColumnExtensionMethodsBuilder =
    profile.createEnumOptionColumnExtensionMethodsBuilder[Gender]

  implicit def partyIdAsSymbioticUserId(pid: PartyId): SymbioticUserId = {
    pid.asInstanceOf[SymbioticUserId]
  }

  implicit def userIdAsSymbioticUserId(uid: UserId): SymbioticUserId = {
    uid.asInstanceOf[SymbioticUserId]
  }

}
