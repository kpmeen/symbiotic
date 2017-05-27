package net.scalytica.symbiotic.api.types

object PartyBaseTypes {

  sealed trait PartyId extends Id

  trait UserId extends PartyId

  trait OrgId extends PartyId

  object UserId extends UserIdOps[UserId] {

    override implicit def asId(s: String) = UserId(s)

    def apply(id: String): UserId = new UserId {
      override val value = id

      override def toString = s"UserId($value)"

      override def equals(obj: scala.Any) = {
        (
          obj.isInstanceOf[PartyId] &&
          obj.asInstanceOf[PartyId].value == this.value
        )
      }

    }
  }

}
