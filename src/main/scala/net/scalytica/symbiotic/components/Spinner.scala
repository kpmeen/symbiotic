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

    val wrapper = styleF(sizeDomain) {
      case Big => styleS(addClassNames("preloader-wrapper", "big", "active"))
      case Medium => styleS(addClassNames("preloader-wrapper", "medium", "active"))
      case Small => styleS(addClassNames("preloader-wrapper", "small", "active"))
    }
    val layer = style(addClassNames("spinner-layer", "spinner-blue-only"))
    val clipper = styleF(alignDomain) {
      case Left => styleS(addClassNames("circle-clipper", "left"))
      case Right => styleS(addClassNames("circle-clipper", "right"))
    }
    val gap = style(addClassNames("gap-patch"))
    val circle = style(addClassNames("circle"))
  }

  val component = ReactComponentB[Props]("Spinner")
    .render(p =>
    <.div(Style.wrapper(p.size),
      <.div(Style.layer,
        <.div(Style.clipper(Left),
          <.div(Style.circle)
        ),
        <.div(Style.gap,
          <.div(Style.circle)
        ),
        <.div(Style.clipper(Right),
          <.div(Style.circle)
        )
      )
    )
    ).build

  def apply() = component(Props(Medium))
  def apply(p: Props) = component(p)
  def apply(size: Size) = component(Props(size))
}
