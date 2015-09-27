/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.User

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object UserProfilePage {

  case class Props(uid: String)

  case class State(uid: String, usr: User = User.empty)

  class Backend(t: BackendScope[Props, State]) {

    def init() = User.getUser(t.props.uid).map {
      case Right(user) => t.modState(_.copy(usr = user))
      case Left(err) => throw new Exception(err.msg)
    }.recover {
      case e: Throwable =>
        log.error(s"Unable to retrieve user data for ${t.props.uid}")
        log.error(s"Reason: $e")
        log.error(e)
    }

  }

  val component = ReactComponentB[Props]("UserProfilePage")
    .initialStateP(p => State(p.uid))
    .backend(new Backend(_))
    .render { (p, s, b) =>
      <.div(^.className := "well",
        <.h2(s"${s.usr.name.map(_.print).getOrElse("NA")}"),
        <.p(
          <.strong("Email:"),
          s.usr.email
        )
      )
    }
    .componentWillMount(_.backend.init())
    .build

  def apply(props: Props) = component(props)

  def apply(uid: String) = component(Props(uid))

}
