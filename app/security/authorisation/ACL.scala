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

  def grant(principal: UserId, permission: Permission): ACL = {
    /*
      TODO: This needs to be slightly more complex.

      if ACE for principal exists...add permission if not already existing
      if ACE for principal does _not_ exist...add new ACE!!!

     */
    val idx = entries.indexWhere(_.principal == principal)
    val ace = entries(idx)
    if (ace.permissions.contains(permission)) this
    else this.copy(entries = entries.updated(idx, ace.copy(permissions = ace.permissions :+ permission)))
  }

  def revoke(principal: UserId, p: Permission): ACL = {
    /*
      TODO: Implement me

      if ACE for principal exist...revoke given permission.
      if ACE for principal is left empty...remove ACE completely
     */
    ???
  }

}

object ACL {
  implicit val f: Format[ACL] = (
    (__ \ "id").format[AclId] and
    (__ \ "entries").format[Seq[ACLEntry]]
  )(ACL.apply, unlift(ACL.unapply))
}