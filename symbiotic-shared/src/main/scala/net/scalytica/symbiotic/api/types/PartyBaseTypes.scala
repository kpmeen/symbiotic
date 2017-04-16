package net.scalytica.symbiotic.api.types

object PartyBaseTypes {

  sealed trait PartyId extends Id

  trait UserId extends PartyId

  trait OrgId extends PartyId

}
