/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.core.facades.Bootstrap._
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.User
import net.scalytica.symbiotic.models.dman.File
import org.scalajs.jquery.jQuery

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.Date
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FileInfo {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style("fileinfo-container")(
      addClassName("container-fluid"),
      width(23.%%),
      position.fixed
    )

    val panelBody = style("fileinfo-panel-body")(
      addClassNames("panel-body", "center-block", "text-center"),
      minHeight(200.px).important,
      width(100.%%).important,
      paddingTop(20.px),
      paddingBottom(20.px)
    )

    val panel = style("fileinfo-panel")(
      addClassNames("panel", "panel-default")
    )

    val title = style("fileinfo-title")(
      addClassNames("center-block", "text-center"),
      fontSize(18.px),
      fontWeight.bold
    )

    val contentType = style("fileinfo-ctype")(
      addClassNames("center-block", "text-center"),
      fontWeight.bold
    )

    val metadata = style("fileinfo-md")(
      addClassNames("row"),
      fontSize(12.px)
    )

    val mdLabel = style("fileinfo-md-label")(
      addClassNames("col-xs-6", "text-right")
    )

    val mdText = style("fileinfo-md-text")(
      addClassNames("col-xs-6", "text-left")
    )
  }

  case class State(maybeFile: ExternalVar[Option[File]], uploadedBy: Option[String] = None)

  class Backend($: BackendScope[ExternalVar[Option[File]], State]) {

    def toReadableDate(ds: String): String = {
      val date = new Date(ds)
      date.toDateString()
    }

    def init(p: ExternalVar[Option[File]]): Callback = Callback {
      p.value.foreach(_.metadata.uploadedBy.foreach { uid =>
        Callback.future[Unit] { User.getUser(uid).map {
          case Left(fail) =>
            log.error(s"Unable to retrieve user data for $uid because: ${fail.msg}")
            Callback.empty
          case Right(usr) =>
            val name: String = usr.name.map { n =>
              s"${n.first.getOrElse("")}${n.middle.map(" " + _).getOrElse("")}${n.last.map(" " + _).getOrElse("")}"
            }.getOrElse(usr.email)
            $.modState(s => State(maybeFile = p, uploadedBy = Some(name)))
        }}.runNow()
      })
    }

    def changeLock(fileId: String, locked: Boolean): Callback =
      $.state.map { s =>
        // TODO: Need to update parent component state
        if (!locked) {
          File.lock(fileId)
        } else {
          File.unlock(fileId)
        }
      }

    def render(state: State) =
      <.div(^.id := "FileInfoAffix", Style.container)(
        <.div(Style.panel,
          state.maybeFile.value.map { fw =>
            val fileId = fw.metadata.fid
            val locked = fw.metadata.lock.isDefined
            val lockBtnLbl = if (locked) "Unlock" else "Lock"
            val lockIcon = if(locked) "fa fa-lock" else "fa fa-unlock"

            <.div(Style.panelBody,
              <.i(FileTypes.Styles.Icon5x(FileTypes.fromContentType(fw.contentType))),
              <.br(),
              <.div(Style.title, <.span(fw.filename)),
              <.br(),
              <.div(Style.contentType,
                <.span(fw.contentType),
                <.span(" - "),
                <.span(s"${fw.length.getOrElse("N/A")} bytes")
              ),
              <.br(),
              <.div(Style.metadata,
                <.label(Style.mdLabel, ^.`for` := s"fi_version_$fileId", "version: "),
                <.span(Style.mdText, ^.name := s"fi_version_$fileId", fw.metadata.version)
              ),
              <.div(Style.metadata,
                <.label(Style.mdLabel, ^.`for` := s"fi_uploaded_$fileId", "uploaded: "),
                <.span(Style.mdText, ^.name := s"fi_uploaded_$fileId", s"${fw.uploadDate.map(toReadableDate).getOrElse("")}")
              ),
              <.div(Style.metadata,
                <.label(Style.mdLabel, ^.`for` := s"fi_by_$fileId", "by: "),
                <.span(Style.mdText, ^.name := s"fi_by_$fileId", s"${state.uploadedBy.getOrElse("")}")
              ),
              <.br(),
              <.div(Style.contentType, <.span(<.i(^.className := lockIcon))),
              fw.metadata.lock.map { l =>
                <.div(
                  <.div(Style.metadata,
                    <.label(Style.mdLabel, ^.`for` := s"fi_lockby_$fileId", "locked by: "),
                    <.span(Style.mdText, ^.name := s"fi_lockby_$fileId", s"${l.by}")
                  ),
                  <.div(Style.metadata,
                    <.label(Style.mdLabel, ^.`for` := s"fi_lockdate_$fileId", "since: "),
                    <.span(Style.mdText, ^.name := s"fi_lockdate_$fileId", s"${toReadableDate(l.date)}")
                  )
                )
              },
              <.br(),
              <.input(
                ^.className := "btn btn-primary",
                ^.`type` := "button",
                ^.value := lockBtnLbl,
                ^.onClick --> changeLock(fileId, locked)
              )
            )
          }.getOrElse(<.div(Style.panelBody, "Select a file to see its metadata"))
        )
      )

  }

  val component = ReactComponentB[ExternalVar[Option[File]]]("FileInfo")
    .initialState_P(p => State(p))
    .renderBackend[Backend]
    .componentDidMount($ => CallbackTo(jQuery("#FileInfoAffix").affix()))
    .componentWillReceiveProps(cwu => cwu.$.backend.init(cwu.nextProps))
    .build

  def apply(fw: ExternalVar[Option[File]]) = component(fw)
}
