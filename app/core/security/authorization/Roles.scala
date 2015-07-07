/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.security.authorization

import play.api.libs.json._

// TODO: This must be re-visited.

sealed trait Role

object Role {

  private[this] object RoleStrings {
    val sysAdmin = "Admin"
    val customerAdmin = "CustomerAdmin"
    val projectAdmin = "ProjectAdmin"
    val mediator = "Mediator"
    val performer = "Performer"
    val participant = "Participant"
  }

  implicit val roleReads: Reads[Role] = __.read[String].flatMap(rs => {
    val roleType = fromStringValue(rs)
    Reads.pure(roleType)
  }).map(r => r: Role)

  implicit val roleWrites: Writes[Role] = Writes { case r: Role => JsString(toStringValue(r))}

  def toStringValue(r: Role): String = r match {
    case ar: Admin => ar match {
      case sa: Sysadmin => RoleStrings.sysAdmin
      case ca: CustomerAdmin => RoleStrings.customerAdmin
      case pa: ProjectAdmin => RoleStrings.projectAdmin
    }
    case ur: UserRole => ur match {
      case me: Mediator => RoleStrings.mediator
      case pe: Performer => RoleStrings.performer
      case pa: Participant => RoleStrings.participant
    }
  }

  def fromStringValue(s: String): Role = s match {
    case RoleStrings.sysAdmin => Sysadmin()
    case RoleStrings.customerAdmin => CustomerAdmin()
    case RoleStrings.projectAdmin => ProjectAdmin()
    case RoleStrings.mediator => Mediator()
    case RoleStrings.performer => Performer()
    case RoleStrings.participant => Participant()
  }

}

sealed trait Admin extends Role

sealed trait UserRole extends Role

case class Sysadmin() extends Admin

case class CustomerAdmin() extends Admin

case class ProjectAdmin() extends Admin

case class Mediator() extends UserRole

case class Performer() extends UserRole

case class Participant() extends UserRole
