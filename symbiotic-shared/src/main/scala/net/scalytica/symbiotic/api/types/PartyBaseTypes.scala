package net.scalytica.symbiotic.api.types

object PartyBaseTypes {

  sealed trait PartyId extends Id

  trait OrgId extends PartyId {
    override def toString = s"OrgId($value)"
  }

  trait UserId extends PartyId {
    override def toString = s"UserId($value)"
  }

  object OrgId extends IdOps[OrgId] {

    override implicit def asId(s: String): OrgId = OrgId(s)

    def apply(id: String): OrgId = new OrgId {
      override val value = id

      override def equals(obj: scala.Any) =
        obj.isInstanceOf[OrgId] &&
          obj.asInstanceOf[OrgId].value == this.value
    }

  }

  object UserId extends IdOps[UserId] {

    override implicit def asId(s: String): UserId = UserId(s)

    def apply(id: String): UserId = new UserId {
      override val value = id

      override def equals(obj: scala.Any) =
        obj.isInstanceOf[UserId] &&
          obj.asInstanceOf[UserId].value == this.value

    }
  }

}
