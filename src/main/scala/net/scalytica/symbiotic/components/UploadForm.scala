/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.routing.SymbioticRouter
import org.scalajs.dom.raw._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object UploadForm {

  object Style extends StyleSheet.Inline {

    import dsl._

    val formStyle = style(
      marginBottom(20.px)
    )

    val btnFile = style(
      addClassNames("btn", "btn-default"),
      position.relative,
      overflow.hidden,
      unsafeChild("input[type=file]")(
        position.absolute,
        top.`0`,
        right.`0`,
        minWidth(100.%%),
        minHeight(100.%%),
        fontSize(100.px),
        textAlign.right,
        filter := "alpha(opacity=0)",
        opacity(0),
        outline.none,
        backgroundColor.white,
        cursor.inherit,
        display.block
      )
    )
  }

  case class Props(
    oid: String,
    folder: Option[String],
    filename: String,
    success: () => Unit
    )

  class Backend(t: BackendScope[Props, Props]) {

    def onFileSelected(e: ReactEventI): Unit = {
      log.debug("Here's the event:")
      log.debug(e)
      log.debug("The file is:")
      log.debug(e.target.files.item(0))
      Option(e.target.files.item(0)).foreach(f =>
        t.modState(_.copy(filename = f.name))
      )
    }

    def onUploadFile(e: ReactEventH): Unit = {
      e.preventDefault()
      val form: HTMLFormElement = e.target.asInstanceOf[HTMLFormElement]
      val url = s"${SymbioticRouter.ServerBaseURI}/document/${t.props.oid}/upload?path=${t.props.folder.getOrElse("/")}"
      val fd = new FormData(form)
      val xhr = new XMLHttpRequest
      xhr.onreadystatechange = (e: Event) => {
        if (xhr.readyState == XMLHttpRequest.DONE) {
          if (xhr.status == 200) {
            log.info(xhr.responseText)
            form.reset()
            t.modState(_.copy(filename = ""))
            t.state.success()
          }
        }
      }
      xhr.open(method = "POST", url = url, async = true)
      xhr.send(fd)
    }
  }

  val component = ReactComponentB[Props]("UploadForm")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render { (p, s, b) =>
      <.form(Style.formStyle, ^.onSubmit ==> b.onUploadFile, ^.encType := "multipart/form-data",
        <.div(^.className := "input-group",
          <.span(^.className := "input-group-btn",
            <.span(Style.btnFile, "Browse",
              <.input(^.`type` := "file", ^.name := "file", ^.onChange ==> b.onFileSelected)
            )
          ),
          <.input(^.`type` := "text", ^.className := "form-control", ^.readOnly := true, ^.value := s.filename),
          <.span(^.className := "input-group-btn",
            <.button(^.`type` := "submit", ^.className := "btn btn-primary", "upload")
          )
        )
      )
    }.build

  def apply(p: Props) = component(p)

  def apply(
    oid: String,
    folder: Option[String] = None,
    success: () => Unit) = component(Props(oid, folder, "", success))
}
