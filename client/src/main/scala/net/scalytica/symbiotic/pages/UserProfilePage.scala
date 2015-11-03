/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.User

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.Date
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object UserProfilePage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val loading = style("profiledata-loading")(
      addClassNames("text-center"),
      height(100.%%),
      width(100.%%)
    )

    val avatar = style("user-avatar")(
      addClassNames("img-circle", "avatar", "avatar-original"),
      display.block,
      margin.auto
    )

    val infoBlock = style("user-info")(
      addClassNames("col-md-8"),
      mixin(unsafeChild("div")(style(
        margin.`0`
      )))
    )

    val profileLabel = style("fileinfo-md-label")(
      addClassNames("text-muted")
    )
  }

  case class Props(uid: String)

  case class State(uid: String, usr: Option[User] = None, status: AjaxStatus = Loading)

  class Backend($: BackendScope[Props, State]) {

    def loadContent(uid: String): Callback = Callback.future[Unit] {
      User.getUser(uid).map {
        case Right(user) =>
          log.debug(s"Found profile data for $uid. Updating state.")
          $.modState(_.copy(usr = Some(user), status = Finished))
        case Left(failed) =>
          log.warn(s"Unable to retrieve user data for $uid. Reason: ${failed.msg}")
          $.modState(_.copy(status = failed))
      }
    }

    def render(state: State) = {
      <.div(^.className := "row",
        <.div(^.className := "col-md-5",
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", "User profile")
            ),
            state.status match {
              case Loading =>
                <.div(^.className := "panel-body",
                  <.div(Style.loading, Spinner(Medium))
                )
              case Finished =>
                state.usr.fold(
                  <.div(^.className := "panel-body", "Could not find any information for you.")
                ) { usr =>
                  <.div(^.className := "panel-body",
                    <.div(^.className := "row",
                      <.div(^.className := "col-md-4 text-center",
                        // TODO: replace with data from "userAvatar" service once implemented.
                        <.img(Style.avatar, ^.src := "http://robohash.org/sitsequiquia.png?size=120x120")
                      ),
                      <.div(Style.infoBlock,
                        <.div(
                          <.h1(^.className := "only-bottom-margin",
                            usr.name.get.first + "\u00a0" + usr.name.get.last
                          )
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_uname_${usr.id.get}", "Username:\u00a0"),
                          <.span(^.id := s"profile_uname_${usr.id.get}", usr.username)
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_email_${usr.id.get}", "Email:\u00a0"),
                          <.span(^.id := s"profile_email_${usr.id.get}", usr.email)
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_dob_${usr.id.get}", "Date of birth:\u00a0"),
                          <.span(^.id := s"profile_dob_${usr.id.get}", usr.dateOfBirth.map[String](d => new Date(d).toDateString()))
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_gender_${usr.id.get}", "Gender:\u00a0"),
                          <.span(^.id := s"profile_gender_${usr.id.get}", usr.readableGender.getOrElse[String](""))
                        )
                      )
                    )
                  )
                }
              case Failed(msg) =>
                <.div(^.className := "panel-body",
                  <.span(s"Ooops, we couldn't get your profile.")
                )
            }
          )
        ),
        <.div(^.className := "col-md-5",
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", "Activity feed")
            ),
            <.div(^.className := "panel-body",
              "column 2"
            )
          )
        ),
        <.div(^.className := "col-md-3",
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", "Something something")
            ),
            <.div(^.className := "panel-body",
              "column 3"
            )
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("UserProfilePage")
    .initialState_P(p => State(p.uid))
    .renderBackend[Backend]
    .componentWillMount(dcu => dcu.backend.loadContent(dcu.props.uid))
    .componentWillReceiveProps(cwu => cwu.$.backend.loadContent(cwu.nextProps.uid))
    .build

  def apply(props: Props) = component(props)

  def apply(uid: String) = component(Props(uid))

}
