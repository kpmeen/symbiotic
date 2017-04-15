package net.scalytica.symbiotic.data

object PartyBaseTypes {

  sealed trait PartyId extends Id

  trait UserId extends PartyId

  trait OrgId extends PartyId

}
