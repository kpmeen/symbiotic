/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.models.party

import net.scalytica.symbiotic.core.http.SymbioticRequest
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.{AuthToken, Name}
import upickle.default._
import net.scalytica.symbiotic.routing.SymbioticRouter.ServerBaseURI
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue


import scala.concurrent.Future

case class CreateUser(
  username: String,
  email: String,
  password1: String,
  password2: String,
  name: Option[Name] = None,
  dateOfBirth: Option[String],
  gender: Option[String] = None
)

object CreateUser {

  val empty = CreateUser(
    username = "",
    email = "",
    password1 = "",
    password2 = "",
    name = None,
    dateOfBirth = None,
    gender = None
  )


  def register(cu: CreateUser): Future[Boolean] = {
    SymbioticRequest.post(
      url = s"$ServerBaseURI/register",
      headers = Map(
        "Accept" -> "application/json",
        "Content-Type" -> "application/json"
      ),
      data = write[CreateUser](cu)
    ).map { xhr =>
      xhr.status match {
        case ok: Int if ok == 201 =>
          val as = read[AuthToken](xhr.responseText)
          Session.init(as)
          true
        case _ =>
          log.error(s"Status ${xhr.status}: ${xhr.statusText}")
          false
      }
    }.recover {
      case err =>
        log.error(err)
        false
    }
  }


}

case class CreateUserValidity(
  usernameValid: Boolean = true,
  emailValid: Boolean = true,
  password1Valid: Boolean = true,
  password2Valid: Boolean = true,
  dateOfBirthValid: Option[Boolean] = None
)

object CreateUserValidity {
  def validateUsername(uname: String): Future[Boolean] =
    SymbioticRequest.get(
      url = s"$ServerBaseURI/validate/username/$uname"
    ).map(_.status == 200).recover {
      case err =>
        log.error(err)
        false
    }
}