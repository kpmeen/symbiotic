/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.css.{FileTypes, Material}
import net.scalytica.symbiotic.models.dman.FileWrapper

import scala.scalajs.js.Date
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FileInfo {

  object Style extends StyleSheet.Inline {

    import dsl._

    val infoContainer = Material.container.compose(style(
      height(100.%%),
      width(100.%%)
    ))

    val title = style(
      Material.centerAlign,
      fontSize(18.px),
      fontWeight.bold
    )

    val contentType = style(
      Material.centerAlign,
      fontWeight.bold
    )

    val metadata = style(
      fontSize(12.px)
    )
  }

  val component = ReactComponentB[ExternalVar[Option[FileWrapper]]]("FileInfo")
    .render { evar =>
    def toReadableDate(ds: String): String = {
      val date = new Date(ds)
      date.toDateString()
    }
    // TODO: Build HTML for displaying metadata information
    <.div(Style.infoContainer,
      <.div(Material.cardDefault,
        evar.value.map(fw =>
          <.div(Material.cardContent,
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
            <.div(Style.metadata, <.b("version: "), <.span(fw.metadata.version)),
            <.div(Style.metadata, <.b("uploaded: "), <.span(s"${fw.uploadDate.map(toReadableDate).getOrElse("")}")),
            <.div(Style.metadata, <.b("by: "), <.span("todo: name of user")) //<.span(s"${fw.uploadedBy.getOrElse("")}"))
          )
        )
      )
    )
  }
    .build

  def apply(fw: ExternalVar[Option[FileWrapper]]) = component(fw)
}
