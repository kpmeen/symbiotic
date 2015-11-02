/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.User

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom.raw.StyleSheet
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.Medium
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scala.scalajs.js.Date

object UserProfilePage {

  case class Props(uid: String)

  case class State(uid: String, usr: User = User.empty)

  class Backend($: BackendScope[Props, State]) {

    def init(uid: String): Callback = Callback.future[Unit] {
      User.getUser(uid).map {
        case Right(user) => $.modState(_.copy(usr = user))
        case Left(err) => 
          log.error(s"Unable to retrieve user data for ${uid}")
          log.error(s"Reason: ${err.msg}")
          log.error(err)
          Callback.empty
      }
    }

    object Style extends StyleSheet.Inline {

      import dsl._

      val loading = style("filecontent-loading")(
        addClassNames("center-block", "text-center"),
        height(100.%%),
        width(100.%%)
      )
    
      val avatar = style(
        addClassNames("img-circle","avatar","avatar-original"),
        display.block,
        margin.auto
      )
    }

    def render(p: Props, state: State) = {
        <.div(^.className := "row",
          <.div(^.className := "col-md-5",
            <.div(^.className := "panel panel-default",
              <.div(^.className := "panel-heading",
                <.h3(^.className := "panel-title", "User profile")
              ),
              if (state.usr == User.empty) {
                <.div(^.className := "container-fluid",
                    <.div(^.className := "panel panel-default",
                      <.div(^.className := "panel-body",
                        <.div(Style.loading, Spinner(Medium))
                      )
                    )
                  )
              } else {
                <.div(^.className := "panel-body",
                    <.div(^.className := "col-md-4 text-center",
                      <.img(Style.avatar, ^.src := "http://robohash.org/sitsequiquia.png?size=120x120")
                    ),
                    <.div(^.className := "col-md-8",
                        <.div(^.className := "row",
                            <.div(^.className := "col-md-12",
                                <.h1(^.className := "only-bottom-margin",
                                    state.usr.name.get.first + "\u00a0" + state.usr.name.get.last
                                )
                            )
                        ),
                        <.div(^.className := "row",
                           <.div(^.className := "col-md-12",
                               <.span(^.className := "text-muted", "Username:"),
                               "\u00a0",
                               state.usr.username,
                               <.br,
                               <.span(^.className := "text-muted", "Email:"),
                               "\u00a0",
                               state.usr.email,
                               <.br,
                               <.span(^.className := "text-muted", "Birth\u00a0date:"),
                               "\u00a0",
                               state.usr.dateOfBirth.map[String](d => new Date(d).toDateString()),
                               <.br,
                               <.span(^.className := "text-muted", "Gender:"),
                               "\u00a0",
                               state.usr.gender.getOrElse("N/A").asInstanceOf[String]
                           )
                        )
                    )
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
    .componentWillMount(dcu => dcu.backend.init(dcu.props.uid))
    .componentWillReceiveProps(cwu => cwu.$.backend.init(cwu.nextProps.uid))
    .build

  def apply(props: Props) = component(props)

  def apply(uid: String) = component(Props(uid))

}
