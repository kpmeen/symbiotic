/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.models.dman.FileWrapper

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FileInfo {

  object Style extends StyleSheet.Inline {

    import dsl._

    val wrapper = style(
      textDecoration := "none"
    )

  }

  case class Props(fw: FileWrapper)

  val component = ReactComponentB[Props]("FileInfo")
    .render(p =>
      <.div(Style.wrapper)(p.fw.filename)
    )

}
