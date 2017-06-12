package net.scalytica.symbiotic.api.types

object PartyBaseTypes {

  sealed trait PartyId extends Id

  // scalastyle:off
  trait UserId extends PartyId {

    override def equals(obj: scala.Any) = {
      obj.isInstanceOf[UserId] && obj.asInstanceOf[UserId].value == this.value
    }

  }

  trait OrgId extends PartyId

  object UserId extends UserIdOps[UserId] {

    override implicit def asId(s: String): UserId = UserId(s)

    def apply(id: String): UserId = new UserId {
      override val value = id

      override def toString = s"UserId($value)"

      override def equals(obj: scala.Any) =
        obj.isInstanceOf[PartyId] &&
          obj.asInstanceOf[PartyId].value == this.value

    }
  }

}
