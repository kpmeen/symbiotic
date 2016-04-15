package net.scalytica.symbiotic.models.party

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra.router.RouterCtl
import net.scalytica.symbiotic.core.http.{Failed, SymbioticRequest}
import net.scalytica.symbiotic.core.session.Session
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models._
import net.scalytica.symbiotic.routing.SymbioticRouter.{Login, ServerBaseURI, View}
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.raw._
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class User(
  v: Option[VersionStamp] = None,
  loginInfo: LoginInfo,
  id: Option[String] = None,
  username: String,
  email: String,
  name: Option[Name] = None,
  dateOfBirth: Option[String] = None,
  gender: Option[String] = None,
  active: Boolean = true,
  avatarUrl: Option[String] = None,
  useSocialAvatar: Boolean = true
) {

  def emailOption: Option[String] = {
    if (email == "not_provided@scalytica.net") None
    else Some(email)
  }

  def readableGender: Option[String] = gender.flatMap {
    case m if m.equals("m") => Some("Male")
    case f if f.equals("f") => Some("Female")
    case _ => None
  }

  def readableName: String =
    name.map { n =>
      s"${n.first.getOrElse("")}${n.middle.map(" " + _).getOrElse("")}${n.last.map(" " + _).getOrElse("")}"
    }.getOrElse(email)
}

object User {

  val empty = User(
    username = "",
    email = "",
    loginInfo = LoginInfo.empty
  )

  def authenticate(provider: String, queryParams: Option[String] = None): Future[Boolean] = {
    SymbioticRequest.get(
      url = s"$ServerBaseURI/authenticate/$provider${queryParams.getOrElse("")}"
    ).map { xhr =>
      xhr.status match {
        case ok: Int if ok == 200 =>
          val as = read[AuthToken](xhr.responseText)
          Session.init(as)
          log.debug(s"Session initialized through $provider")
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

  def login(creds: Credentials): Future[Boolean] = {
    SymbioticRequest.post(
      url = s"$ServerBaseURI/login",
      headers = Map(
        "Accept" -> "application/json",
        "Content-Type" -> "application/json"
      ),
      data = s"""{ "username": "${creds.uname}", "password": "${creds.pass}", "rememberMe": false }"""
    ).map { xhr =>
      xhr.status match {
        case ok: Int if ok == 200 =>
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

  def logout(ctl: RouterCtl[View]): Unit =
    SymbioticRequest.get(url = s"$ServerBaseURI/logout").map { xhr =>
      // We don't care what the response status is...we'll remove the cookie anyway
      Session.clear()
      ctl.set(Login).toIO.unsafePerformIO()
    }.recover {
      case e: Exception =>
        Session.clear()
        ctl.set(Login).toIO.unsafePerformIO()
    }

  def currentUser: Future[Either[Failed, User]] = fetchUser(s"$ServerBaseURI/user/current")

  def getUser(uid: String): Future[Either[Failed, User]] = fetchUser(s"$ServerBaseURI/user/$uid")

  private def fetchUser(url: String): Future[Either[Failed, User]] =
    for {
      xhr <- SymbioticRequest.get(
        url = url,
        headers = Map(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        )
      )
    } yield {
      xhr.status match {
        case ok: Int if ok == 200 =>
          val u = read[User](xhr.responseText)
          Right(u)
        case ko =>
          log.warn(s"There was a problem locating the user.")
          Left(Failed(s"${xhr.status} ${xhr.statusText}: ${xhr.responseText}"))
      }
    }

  def getAvatar(uid: String): Future[Option[Blob]] =
    (for {
      xhr <- SymbioticRequest.get(url = s"$ServerBaseURI/user/$uid/avatar", responseType = "blob")
    } yield {
      xhr.status match {
        case ok: Int if ok == 200 =>
          Some(xhr.response.asInstanceOf[Blob])
        case ko =>
          log.warn(s"Unrecognized status code $ko from getAvatar service.")
          None
      }
    }).recover {
      case err =>
        log.info("User has not uploaded an Avatar image")
        None
    }

  def setAvatar(uid: String, form: HTMLFormElement)(done: Callback): Unit = {
    val fd = new FormData(form)
    val xhr = new XMLHttpRequest
    xhr.onreadystatechange = (e: Event) => {
      if (xhr.readyState == XMLHttpRequest.DONE && xhr.status == 200) {
        done.runNow()
      }
    }
    xhr.open(method = "POST", url = s"$ServerBaseURI/user/$uid/avatar", async = true)
    Session.token.foreach(t => xhr.setRequestHeader(SymbioticRequest.XAuthTokenHeader, t.token))
    xhr.send(fd)
  }
}
