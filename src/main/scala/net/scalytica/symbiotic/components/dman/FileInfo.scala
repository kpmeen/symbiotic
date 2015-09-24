/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.models.dman.File

import scala.scalajs.js.Date
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FileInfo {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style("fileinfo-container")(
      addClassName("container-fluid")
    )

    val panelBody = style(
      addClassNames("panel-body", "center-block", "text-center"),
      minHeight(200.px).important,
      minWidth(100.px).important
    )

    val panel = style(
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
      addClassNames("text-left row"),
      fontSize(12.px)
    )
  }

  val component = ReactComponentB[ExternalVar[Option[File]]]("FileInfo").render { evar =>
    def toReadableDate(ds: String): String = {
      val date = new Date(ds)
      date.toDateString()
    }
    // TODO: Build HTML for displaying metadata information
    <.div(Style.container, "data-spy".reactAttr := "affix")(
      <.div(Style.panel,
        evar.value.map(fw =>
          <.div(Style.panelBody,
            <.i(FileTypes.Styles.Icon5x(FileTypes.fromContentType(fw.contentType))),
            <.br(),
            <.div(Style.title, <.span(fw.filename)),
            <.br(),
            <.div(Style.contentType,
              <.span(fw.contentType),
              <.span(" - "),
              <.span(s"${fw.size.getOrElse("")} bytes")
            ),
            <.br(),
            <.div(Style.metadata,
              <.label(^.className := "col-xs-4", ^.`for` := s"fi_version_${fw.id}", "version: "),
              <.span(^.className := "col-xs-8", ^.id := s"fi_version_${fw.id}", fw.metadata.version)
            ),
            <.div(Style.metadata,
              <.label(^.className := "col-xs-4", ^.`for` := s"fi_uploaded_${fw.id}", "uploaded: "),
              <.span(^.className := "col-xs-8", ^.id := s"fi_uploaded_${fw.id}", s"${fw.uploadDate.map(toReadableDate).getOrElse("")}")
            ),
            <.div(Style.metadata,
              <.label(^.className := "col-xs-4", ^.`for` := s"fi_by_${fw.id}", "by: "),
              <.span(^.className := "col-xs-8", ^.id := s"fi_by_${fw.id}", "todo: name of user")
            )
          )
        ).getOrElse(<.div(Style.panelBody, "Select a file to see its metadata"))
      )
    )
  }.build

  def apply(fw: ExternalVar[Option[File]]) = component(fw)
}
