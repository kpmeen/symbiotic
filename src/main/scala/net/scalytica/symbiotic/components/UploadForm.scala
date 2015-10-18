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
    success: () => Callback)

  class Backend($: BackendScope[Props, Props]) {

    def onFileSelected(e: ReactEventI): Callback = {
      log.debug("Here's the event:")
      log.debug(e)
      log.debug("The file is:")
      log.debug(e.target.files.item(0))
      Option(e.target.files.item(0)).map(f =>
        $.modState(_.copy(filename = f.name))
      ).getOrElse(Callback.log("Noe file was selected."))
    }

    def onUploadFile(e: ReactEventH): Callback = {
      e.preventDefaultCB >>
        CallbackTo {
          val state = $.accessDirect.state
          $.props.map { props =>
            val form: HTMLFormElement = e.target.asInstanceOf[HTMLFormElement]
            val url = s"${SymbioticRouter.ServerBaseURI}/document/${props.oid}/upload?path=${props.folder.getOrElse("/")}"
            val fd = new FormData(form)
            val xhr = new XMLHttpRequest
            xhr.onreadystatechange = (e: Event) => {
              if (xhr.readyState == XMLHttpRequest.DONE) {
                if (xhr.status == 200) {
                  log.info(xhr.responseText)
                  form.reset()
                  state.success().runNow()
                  $.modState(_.copy(filename = "")).runNow()
                }
              }
            }
            xhr.open(method = "POST", url = url, async = true)
            xhr.send(fd)
          }
        }.flatten
    }

    def render(state: Props) = {
      <.form(Style.formStyle, ^.onSubmit ==> onUploadFile, ^.encType := "multipart/form-data",
        <.div(^.className := "input-group",
          <.span(^.className := "input-group-btn",
            <.span(Style.btnFile, "Browse",
              <.input(^.`type` := "file", ^.name := "file", ^.onChange ==> onFileSelected)
            )
          ),
          <.input(^.`type` := "text", ^.className := "form-control", ^.readOnly := true, ^.value := state.filename),
          <.span(^.className := "input-group-btn",
            <.button(^.`type` := "submit", ^.className := "btn btn-primary", "upload")
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("UploadForm")
    .initialState_P(p => p)
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)

  def apply(
    oid: String,
    folder: Option[String] = None,
    success: () => Callback) = component(Props(oid, folder, "", success))
}
