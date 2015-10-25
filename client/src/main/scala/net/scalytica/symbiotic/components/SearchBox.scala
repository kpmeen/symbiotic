/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js
import scalacss.Defaults._

object SearchBox {

  object Style extends StyleSheet.Inline {

  }

  class Backend($: BackendScope[Props, _]) {
    def onChange(e: ReactEventI) = {
      e.preventDefaultCB >>
        $.props.flatMap(_.onTextChange(e.target.value))
    }

    def render(p: Props) = {
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-body disabled",
          <.input(
            ^.id := p.id,
            ^.className := "form-control",
            ^.`type` := "text",
            ^.placeholder := s"${p.label}...",
            ^.onKeyUp ==> onChange
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("ReactSearchBox")
    .stateless
    .renderBackend[Backend]
    .build

  case class Props(id: String, label: String, onTextChange: String => Callback)

  def apply(id: String, label: String, onTextChange: String => Callback, ref: js.UndefOr[String] = "", key: js.Any = {}) =
    component.set(key, ref)(Props(id, label, onTextChange))

}
