/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.{WithBSONConverters, WithMongo}
import models.base.Username
import models.customer.CustomerId
import models.parties.{OrganizationId, UserId}
import org.bson.types.ObjectId
import play.api.libs.json.{Format, Json}
import security.authorization.Role

/**
 * Represents a user involvement in a project. The following constraints apply:
 *
 * - 1 user can have >= 1 project membership
 * - 1 membership must have a unique combination of uid + cid + pid + oid
 *
 */
case class Membership(
  id: Option[MembershipId],
  uid: UserId,
  uname: Username,
  cid: CustomerId,
  pid: ProjectId,
  oid: OrganizationId,
  roles: Seq[Role] = Seq.empty[Role])


object Membership extends WithMongo with WithBSONConverters[Membership] {

  override val collectionName: String = "project_memberships"

  implicit val memFormat: Format[Membership] = Json.format[Membership]

  override implicit def toBSON(m: Membership): DBObject = {
    val builder = MongoDBObject.newBuilder

    m.id.foreach(builder += "_id" -> _.id)
    builder += "uid" -> m.uid.id
    builder += "uname" -> m.uname.value
    builder += "cid" -> m.cid.id
    builder += "pid" -> m.pid.id
    builder += "oid" -> m.oid.id
    builder += "roles" -> m.roles.map(Role.toStringValue)

    builder.result()
  }

  override implicit def fromBSON(d: DBObject): Membership = {
    Membership(
      id = Option(MembershipId(d.as[ObjectId]("_id"))),
      uid = UserId(d.as[ObjectId]("uid")),
      uname = Username(d.as[String]("uname")),
      cid = CustomerId(d.as[ObjectId]("cid")),
      pid = ProjectId(d.as[ObjectId]("pid")),
      oid = OrganizationId(d.as[ObjectId]("oid")),
      roles = d.as[Seq[String]]("roles").map(Role.fromStringValue)
    )
  }
}