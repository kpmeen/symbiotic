package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}

case object ResourceParties {

  sealed abstract class Type(val tpe: String)
  case object Usr extends Type("user")
  case object Org extends Type("org")

  object Type {

    implicit def fromString(tpe: String): Type = {
      tpe match {
        case Usr.tpe => Usr
        case Org.tpe => Org
        case err =>
          throw new IllegalArgumentException(
            s"Cannot convert $err to a valid Type"
          )
      }
    }

  }

  sealed trait ResourcePartyType {
    val id: PartyId
    val tpe: Type
  }

  case class Owner(id: PartyId, tpe: Type) extends ResourcePartyType

  object Owner {

    def apply(id: PartyId): Owner = id match {
      case uid: UserId => Owner(uid, Usr)
      case oid: OrgId  => Owner(oid, Org)
    }

    def apply(idStr: String, otStr: String): Owner =
      Type.fromString(otStr) match {
        case Usr => Owner(UserId.asId(idStr), Usr)
        case Org => Owner(OrgId.asId(idStr), Org)
      }

  }

  case class AllowedParty(id: PartyId, tpe: Type) extends ResourcePartyType

  object AllowedParty {

    def apply(id: PartyId): AllowedParty = id match {
      case uid: UserId => AllowedParty(uid, Usr)
      case oid: OrgId  => AllowedParty(oid, Org)
    }

    def apply(idStr: String, otStr: String): AllowedParty =
      Type.fromString(otStr) match {
        case Usr => AllowedParty(UserId.asId(idStr), Usr)
        case Org => AllowedParty(OrgId.asId(idStr), Org)
      }
  }

}
