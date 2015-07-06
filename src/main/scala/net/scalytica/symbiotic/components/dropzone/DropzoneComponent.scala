/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dropzone

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import net.scalytica.symbiotic.routes.SymbioticRouter.{ServerBaseURI, TestCID}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object DropzoneComponent {

  object Style extends StyleSheet.Inline {

    import dsl._

    val dz = style(addClassName("dropzone"))
  }

  val component = ReactComponentB[Unit]("Dropzone")
    .stateless
    .render { $ =>
      <.form(Style.dz, ^.action := s"$ServerBaseURI/document/$TestCID/upload?path=/root/k8DN0kTewE-1/k8DN0kTewE-2")
    }.buildU

  def apply() = component()
}
