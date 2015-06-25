/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object SearchBox {

  object Style extends StyleSheet.Inline {

    import dsl._

    val searchBox = style(
      addClassNames("input-field", "col", "s5")
    )
  }

  class Backend(t: BackendScope[Props, _]) {
    def onTextChange(e: ReactEventI) = {
      e.preventDefault()
      t.props.onTextChange(e.target.value)
    }
  }

  val component = ReactComponentB[Props]("ReactSearchBox")
    .stateless
    .backend(new Backend(_))
    .render((p, s, b) => {
      <.div(Style.searchBox,
        <.input(^.id := p.id, ^.className := "validate", ^.`type` := "text",^.onKeyUp ==> b.onTextChange),
        <.label(^.`for` := p.id, p.label)
      )
    })
    .build

  case class Props(id: String, label: String, onTextChange: String => Unit)

  def apply(id: String, label: String, onTextChange: String => Unit, ref: js.UndefOr[String] = "", key: js.Any = {}) =
    component.set(key, ref)(Props(id, label, onTextChange))

}
