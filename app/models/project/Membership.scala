/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.WithObjectBSONConverters
import core.mongodb.WithMongo
import core.security.authorization.Role
import models.base.Username
import models.customer.CustomerId
import models.parties.{OrganizationId, UserId}
import org.bson.types.ObjectId
import play.api.libs.json.{Format, Json}

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


object Membership extends WithMongo with WithObjectBSONConverters[Membership] {

  override val collectionName: String = "project_memberships"

  implicit val memFormat: Format[Membership] = Json.format[Membership]

  override def toBSON(m: Membership): DBObject = {
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

  override def fromBSON(d: DBObject): Membership = {
    Membership(
      id = d.getAs[ObjectId]("_id"),
      uid = d.as[ObjectId]("uid"),
      uname = Username(d.as[String]("uname")),
      cid = d.as[ObjectId]("cid"),
      pid = d.as[ObjectId]("pid"),
      oid = d.as[ObjectId]("oid"),
      roles = d.as[Seq[String]]("roles").map(Role.fromStringValue)
    )
  }
}