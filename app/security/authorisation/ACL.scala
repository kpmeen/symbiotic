/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import com.mongodb.casbah.Imports._
import models.parties.UserId
import play.api.libs.json._

// TODO: Comment me
case class ACL(entries: Seq[ACLEntry] = Seq.empty) {

  def ++(ace: ACLEntry): ACL = this.copy(entries = entries :+ ace)

  def --(ace: ACLEntry): ACL = this.copy(entries = entries.filterNot(_ == ace))

  def grant(principal: UserId, permission: Permission): ACL =
    entries.zipWithIndex.find(f => f._1.principal == principal).map { ai =>
      ai._1.permissions.find(_ == permission).map(_ => this).getOrElse(
        this.copy(entries = entries.updated(ai._2, ai._1.copy(permissions = ai._1.permissions + permission)))
      )
    }.getOrElse(this ++ ACLEntry(principal, Set(permission)))

  def revoke(principal: UserId, permission: Permission): ACL =
    entries.zipWithIndex.find(f => f._1.principal == principal).flatMap { ai =>
      ai._1.permissions.find(_ == permission).map { perm =>
        val perms = ai._1.permissions - permission
        if (perms.isEmpty) revokeAll(principal)
        else this.copy(entries = entries.updated(ai._2, ai._1.copy(permissions = perms)))
      }
    }.getOrElse(this)

  def revokeAll(principal: UserId): ACL = this.copy(entries = entries.filterNot(_.principal == principal))

  def find(principal: UserId): Option[ACLEntry] = entries.find(_.principal == principal)

}

object ACL {

  implicit val f: Format[ACL] = Json.format[ACL]

  def toBSON(x: ACL): DBObject =
    MongoDBObject("entries" -> x.entries.map(ace => ACLEntry.toBSON(ace)))

  def fromBSON(dbo: DBObject): ACL =
    ACL(dbo.as[MongoDBList]("entries").map(dbo => ACLEntry.fromBSON(dbo.asInstanceOf[DBObject])))

}

// TODO: Comment me
case class ACLEntry(principal: UserId, permissions: Set[Permission])

object ACLEntry {
  implicit val aceFormat: Format[ACLEntry] = Json.format[ACLEntry]

  def toBSON(ace: ACLEntry): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "principal" -> ace.principal.value
    builder += "permissions" -> ace.permissions.map(Permission.asString)
    builder.result()
  }

  def fromBSON(dbo: DBObject): ACLEntry =
    ACLEntry(
      principal = UserId(dbo.as[String]("principal")),
      permissions = dbo.as[MongoDBList]("permissions")
        .map(p => Permission.fromString(p.asInstanceOf[String]))
        .filter(_.isDefined)
        .map(_.get)
        .toSet
    )
}