/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.univeq.UnivEq
import net.scalytica.symbiotic.css.FontAwesome

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object Spinner {

  sealed trait Size

  case object Big extends Size

  case object Medium extends Size

  case object Small extends Size

  implicit def univEq: UnivEq[Size] = UnivEq.derive

  case class Props(size: Size)

  object Style extends StyleSheet.Inline {

    import dsl._

    val sizeDomain = Domain.ofValues[Size](Big, Medium, Small)

    val spinner = styleF(sizeDomain) {
      case Big =>
        styleS(FontAwesome.spinner, FontAwesome.size5x, FontAwesome.pulse)
      case Medium =>
        styleS(FontAwesome.spinner, FontAwesome.size3x, FontAwesome.pulse)
      case Small =>
        styleS(FontAwesome.spinner, FontAwesome.pulse)
    }
  }

  val component = ReactComponentB[Props]("Spinner").render($ => <.i(Style.spinner($.props.size))).build

  def apply() = component(Props(Medium))

  def apply(p: Props) = component(p)

  def apply(size: Size) = component(Props(size))
}
