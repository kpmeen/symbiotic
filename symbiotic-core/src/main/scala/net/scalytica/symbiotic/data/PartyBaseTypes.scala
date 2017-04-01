package net.scalytica.symbiotic.data

object PartyBaseTypes {

  sealed trait PartyId extends Id

  /**
   * Id implementation to be used for identifying Users
   */
  case class UserId(value: String) extends PartyId

  object UserId extends IdConverters[UserId] {

    override implicit def asId(s: String): UserId = UserId(s)
  }

}
