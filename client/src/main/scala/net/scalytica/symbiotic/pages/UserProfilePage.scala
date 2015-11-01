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

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object UserProfilePage {

  case class Props(uid: String)

  case class State(uid: String, usr: User = User.empty)

  class Backend($: BackendScope[Props, State]) {

    def init(): Callback = $.props.map(p =>
      User.getUser(p.uid).map {
        case Right(user) => $.modState(_.copy(usr = user))
        case Left(err) => throw new Exception(err.msg)
      }.recover {
        case e: Throwable =>
          log.error(s"Unable to retrieve user data for ${p.uid}")
          log.error(s"Reason: $e")
          log.error(e)
      }
    )

    object Style extends StyleSheet.Inline {

      import dsl._

      val avatar = style(
        addClassNames("img-circle","avatar","avatar-original"),
        display.block,
        margin.auto
      )
    }

    def render(p: Props) = {
      <.div(^.className := "row",
        <.div(^.className := "col-md-5",
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", "User profile")
            ),
            <.div(^.className := "panel-body",
              <.div(^.className := "col-md-4 text-center",
                <.img(Style.avatar, ^.src := "http://robohash.org/sitsequiquia.png?size=120x120")
              ),
              <.div(^.className := "col-md-8",
                  <.div(^.className := "row",
                      <.div(^.className := "col-md-12",
                          <.h1(^.className := "only-bottom-margin",
                              "Joseph Smith"
                          )
                      )
                  ),
                  <.div(^.className := "row",
                     <.div(^.className := "col-md-6",
                         <.span(^.className := "text-muted", "Email:"),
                         "\u00a0",
                         "email@test.com",
                         <.br,
                         <.span(^.className := "text-muted", "Birth\u00a0date:"),
                         "\u00a0",
                         "01.01.2001",
                         <.br,
                         <.span(^.className := "text-muted", "Gender:"),
                         "\u00a0",
                         "male",
                         <.br,
                         <.span(^.className := "text-muted", "Created:"),
                         "\u00a0",
                         "01.01.2015",
                         <.br
                     )
                  )
              )
            )
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
    .componentWillMount(_.backend.init())
    .build

  def apply(props: Props) = component(props)

  def apply(uid: String) = component(Props(uid))

}
