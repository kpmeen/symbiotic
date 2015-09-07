/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import models.parties.UserId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ACL(id: AclId, entries: Seq[ACLEntry] = Seq.empty) {

  def ++(ace: ACLEntry): ACL = this.copy(entries = entries :+ ace)

  def --(ace: ACLEntry): ACL = this.copy(entries = entries.filterNot(_ == ace))

  def add(ace: ACLEntry): ACL = ++(ace)

  def remove(ace: ACLEntry): ACL = --(ace)

  def grant(principal: UserId, permission: Permission): ACL =
    entries.zipWithIndex.find(f => f._1.principal == principal).map { ai =>
      ai._1.permissions.find(_ == permission).map(_ => this).getOrElse(
        this.copy(entries = entries.updated(ai._2, ai._1.copy(permissions = ai._1.permissions :+ permission)))
      )
    }.getOrElse(this ++ ACLEntry(principal, Seq(permission)))

  def revoke(principal: UserId, permission: Permission): ACL =
    entries.zipWithIndex.find(f => f._1.principal == principal).flatMap { ai =>
      ai._1.permissions.find(_ == permission).map { perm =>
        val perms = ai._1.permissions.filterNot(_ == permission)
        if (perms.isEmpty) this.copy(entries = entries.filterNot(ace => ace.principal == principal))
        else this.copy(entries = entries.updated(ai._2, ai._1.copy(permissions = perms)))
      }
    }.getOrElse(this)

}

object ACL {
  implicit val f: Format[ACL] = (
    (__ \ "id").format[AclId] and
    (__ \ "entries").format[Seq[ACLEntry]]
  )(ACL.apply, unlift(ACL.unapply))
}