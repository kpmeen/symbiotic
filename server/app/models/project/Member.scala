/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.project

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import core.converters.ObjectBSONConverters
import core.security.authorisation.Role
import models.base.PersistentType.VersionStamp
import models.base.{PersistentType, PersistentTypeConverters, Username}
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import org.bson.types.ObjectId
import play.api.libs.json.{Format, Json}

/**
 * Represents a user involvement in a project. The following constraints apply:
 *
 * - 1 user can have >= 1 project membership
 * - 1 membership must have a unique combination of uid + orgId + pid
 *
 */
case class Member(
  _id: Option[ObjectId],
  v: Option[VersionStamp],
  id: Option[MemberId],
  uid: UserId,
  uname: Username,
  orgId: OrganisationId,
  pid: ProjectId,
  represents: Option[OrganisationId] = None,
  roles: Seq[Role] = Seq.empty[Role]
) extends PersistentType

object Member extends PersistentTypeConverters with ObjectBSONConverters[Member] {

  implicit val memFormat: Format[Member] = Json.format[Member]

  implicit override def toBSON(m: Member): DBObject = {
    val builder = MongoDBObject.newBuilder

    m._id.foreach(builder += "_id" -> _)
    m.v.foreach(builder += "v" -> VersionStamp.toBSON(_))
    m.id.foreach(builder += "id" -> _.value)
    builder += "uid" -> m.uid.value
    builder += "uname" -> m.uname.value
    builder += "orgId" -> m.orgId.value
    builder += "pid" -> m.pid.value
    m.represents.foreach(builder += "represents" -> _.value)
    builder += "roles" -> m.roles.map(Role.toStringValue)

    builder.result()
  }

  implicit override def fromBSON(d: DBObject): Member = {
    Member(
      _id = d.getAs[ObjectId]("_id"),
      v = d.getAs[DBObject]("v").map(VersionStamp.fromBSON),
      id = d.getAs[String]("id"),
      uid = d.as[String]("uid"),
      uname = Username(d.as[String]("uname")),
      orgId = d.as[String]("orgId"),
      pid = d.as[String]("pid"),
      represents = d.getAs[String]("represents"),
      roles = d.as[Seq[String]]("roles").map(Role.fromStringValue)
    )
  }
}