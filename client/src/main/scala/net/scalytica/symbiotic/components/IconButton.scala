/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object IconButton {

  object Style extends StyleSheet.Inline {

    import dsl._

    val defaultButton = style("icon-btn-default")(
      addClassNames("btn", "btn-default")
    )
  }

  case class Props(iconCls: String, onPress: (ReactEventI) => Callback)

  val component = ReactComponentB[Props]("UploadForm")
    .stateless
    .render_P { $ =>
      <.button(^.`type` := "button", Style.defaultButton, ^.onClick ==> $.onPress,
        <.i(^.className := $.iconCls)
      )
    }
    .build

  def apply(props: Props) = component(props)

  def apply(iconCls: String, onPress: (ReactEventI) => Callback) = component(Props(iconCls, onPress))

  def apply(iconCls: String) = component(Props(iconCls, _ => Callback.empty))

}
