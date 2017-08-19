package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}

case object ResourceOwner {

  sealed abstract class OwnerType(val tpe: String)
  case object UserOwner extends OwnerType("user")
  case object OrgOwner  extends OwnerType("org")

  object OwnerType {

    implicit def fromString(tpe: String): OwnerType = {
      tpe match {
        case UserOwner.tpe => UserOwner
        case OrgOwner.tpe  => OrgOwner
        case err =>
          throw new IllegalArgumentException(
            s"Cannot convert $err to a valid OwnerType"
          )
      }
    }

  }

  case class Owner(id: PartyId, ownerType: OwnerType)

  object Owner {

    def apply(id: PartyId): Owner = id match {
      case uid: UserId => Owner(uid, UserOwner)
      case oid: OrgId  => Owner(oid, OrgOwner)
    }

    def apply(idStr: String, otStr: String): Owner =
      OwnerType.fromString(otStr) match {
        case UserOwner => Owner(UserId.asId(idStr), UserOwner)
        case OrgOwner  => Owner(OrgId.asId(idStr), OrgOwner)
      }

  }

}
