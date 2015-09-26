/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object Spinner {

  sealed trait Size

  case object Big extends Size

  case object Medium extends Size

  case object Small extends Size

  sealed trait Align

  case object Left extends Align

  case object Right extends Align

  case class Props(size: Size)

  object Style extends StyleSheet.Inline {

    import dsl._

    val sizeDomain = Domain.ofValues[Size](Big, Medium, Small)
    val alignDomain = Domain.ofValues[Align](Left, Right)

    val spinner = styleF(sizeDomain) {
      case Big => styleS(addClassNames("fa", "fa-spinner", "fa-5x", "fa-pulse"))
      case Medium => styleS(addClassNames("fa", "fa-spinner", "fa-3x", "fa-pulse"))
      case Small => styleS(addClassNames("fa", "fa-spinner", "fa-pulse"))
    }
  }

  val component = ReactComponentB[Props]("Spinner").render(p => <.i(Style.spinner(p.size))).build

  def apply() = component(Props(Medium))

  def apply(p: Props) = component(p)

  def apply(size: Size) = component(Props(size))
}
