package net.scalytica.symbiotic.models

import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra.router.RouterCtl
import net.scalytica.symbiotic.core.http.Failed
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.routing.SymbioticRouter.{Login, ServerBaseURI, View}
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.Blob
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case class Credentials(uname: String, pass: String)

case class User(
  v: Option[VersionStamp] = None,
  id: Option[String] = None,
  username: String,
  email: String,
  password: String,
  name: Option[Name] = None,
  dateOfBirth: Option[String] = None,
  gender: Option[String] = None,
  active: Boolean = true) {

  def readableGender: Option[String] = gender.flatMap {
    case m if m.equals("m") => Some("Male")
    case f if f.equals("f") => Some("Female")
    case _ => None
  }

}

object User {

  val empty = User(
    username = "",
    email = "",
    password = ""
  )

  def login(creds: Credentials): Future[XMLHttpRequest] =
    Ajax.post(
      url = s"$ServerBaseURI/login",
      headers = Map(
        "Accept" -> "application/json",
        "Content-Type" -> "application/json"
      ),
      data = s"""{ "username": "${creds.uname}", "password": "${creds.pass}" }"""
    )

  def logout(ctl: RouterCtl[View]): Unit =
    Ajax.get(url = s"$ServerBaseURI/logout").map { xhr =>
      // We don't care what the response status is...we'll remove the cookie anyway
      Session.clear()
      ctl.set(Login).toIO.unsafePerformIO()
    }

  def getUser(uid: String): Future[Either[Failed, User]] =
    Ajax.get(
      url = s"$ServerBaseURI/user/$uid",
      headers = Map(
        "Accept" -> "application/json",
        "Content-Type" -> "application/json"
      )
    ).map { xhr =>
      if (xhr.status >= 200 && xhr.status < 400) {
        val u = read[User](xhr.responseText)
        Right(u)
      }
      else {
        Left(Failed(s"${xhr.status} ${xhr.statusText}: ${xhr.responseText}"))
      }
    }

  def getAvatar(uid: String): Future[Option[Blob]] =
    Ajax.get(url = s"$ServerBaseURI/user/$uid/avatar", responseType = "blob").map { xhr =>
      if (xhr.status >= 200 && xhr.status < 400) {
        Some(xhr.response.asInstanceOf[Blob])
      } else {
        None
      }
    }
}
