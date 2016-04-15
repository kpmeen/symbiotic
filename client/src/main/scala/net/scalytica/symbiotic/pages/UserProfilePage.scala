/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.components.Spinner
import net.scalytica.symbiotic.components.Spinner.Medium
import net.scalytica.symbiotic.core.dom.URL
import net.scalytica.symbiotic.core.http.{AjaxStatus, Failed, Finished, Loading}
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.LoginInfo
import net.scalytica.symbiotic.models.party.User
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLFormElement
import org.scalajs.jquery.jQuery

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object UserProfilePage {

  object Style extends StyleSheet.Inline {

    import dsl._

    val loading = style("profiledata-loading")(
      addClassNames("text-center"),
      height(100 %%),
      width(100 %%)
    )

    val avatar = style("user-avatar")(
      addClassNames("img-circle", "avatar", "avatar-original"),
      width(120 px),
      height(120 px),
      display block,
      margin auto,
      cursor pointer
    )

    val infoBlock = style("user-info")(
      addClassNames("col-md-8"),
      mixin(unsafeChild("div")(style(
        margin `0`
      )))
    )

    val profileLabel = style("fileinfo-md-label")(
      addClassNames("text-muted")
    )
  }

  case class Props(uid: String)

  case class Avatar(url: Option[String] = None, blob: Option[dom.Blob] = None) {
    val defaultImageUrl = "http://robohash.org/sitsequiquia.png?size=120x120"

    private def blobToObjURL: Option[String] = blob.map(b => URL.createObjectURL(b))

    def calc(preferSocial: Boolean): String =
      if (preferSocial) url.getOrElse(blobToObjURL.getOrElse(defaultImageUrl))
      else blobToObjURL.getOrElse(url.getOrElse(defaultImageUrl))

  }

  case class State(
    uid: String,
    usr: Option[User] = None,
    avatar: Avatar = Avatar(),
    status: AjaxStatus = Loading
  )

  class Backend($: BackendScope[Props, State]) {

    def loadContent(uid: String): Callback =
      Callback.future[Unit] {
        User.getUser(uid).flatMap {
          case Right(user) =>
            log.debug(s"Found profile data for $uid.")
            User.getAvatar(uid).map { ma =>
              $.modState(s => s.copy(usr = Some(user), avatar = Avatar(user.avatarUrl, ma), status = Finished))
            }
          case Left(failed) =>
            log.warn(s"Unable to retrieve user data for $uid. Reason: ${failed.msg}")
            Future.successful($.modState(s => s.copy(usr = None, avatar = Avatar(), status = failed)))
        }
      }

    def showFileDialogue: Callback = Callback(jQuery("input[name=avatarFile]").click())

    def changeAvatar(e: ReactEventI): Callback = {
      val form = e.currentTarget.parentElement.asInstanceOf[HTMLFormElement]
      val file = e.currentTarget.files.item(0)
      $.state.map { s =>
        User.setAvatar(s.uid, form)($.modState(s => s.copy(avatar = s.avatar.copy(blob = Option(file)))))
      }
    }

    def render(state: State) = {
      <.div(^.className := "row",
        <.div(^.className := "col-md-4",
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
                        <.form(^.name := "avatarForm", ^.encType := "multipart/form-data",
                          <.input(
                            ^.name := "avatarFile",
                            ^.`type` := "file",
                            ^.accept := "image/jpeg,image/png,image/svg+xml,image/tiff",
                            ^.visibility.hidden,
                            ^.onChange ==> changeAvatar
                          ),
                          <.img(Style.avatar, ^.src := state.avatar.calc(usr.useSocialAvatar), ^.onClick --> showFileDialogue)
                        )
                      ),
                      <.div(Style.infoBlock,
                        <.div(
                          <.h1(^.className := "only-bottom-margin", usr.readableName)
                        ),
                        // Only show the username if the user actually created it through the registration process.
                        // Social usernames can be...well...weird...or just the same as the email.
                        Option(usr.loginInfo.providerID).filter(_ == LoginInfo.credentialsProvider).map(_ =>
                          <.div(
                            <.label(Style.profileLabel, ^.`for` := s"profile_uname_${usr.id.get}", "Username:\u00a0"),
                            <.span(^.name := s"profile_uname_${usr.id.get}", usr.username)
                          )
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_email_${usr.id.get}", "Email:\u00a0"),
                          <.span(^.name := s"profile_email_${usr.id.get}", usr.emailOption)
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_dob_${usr.id.get}", "Date of birth:\u00a0"),
                          <.span(^.name := s"profile_dob_${usr.id.get}", usr.dateOfBirth.map[String](d => new Date(d).toDateString()))
                        ),
                        <.div(
                          <.label(Style.profileLabel, ^.`for` := s"profile_gender_${usr.id.get}", "Gender:\u00a0"),
                          <.span(^.name := s"profile_gender_${usr.id.get}", usr.readableGender.getOrElse[String](""))
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
