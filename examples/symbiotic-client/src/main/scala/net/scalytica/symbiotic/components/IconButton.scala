package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object IconButton {

  object Style extends StyleSheet.Inline {

    import dsl._

    val defaultButton =
      style("icon-btn-default")(addClassNames("btn", "btn-default"))
  }

  case class Props(
      iconCls: String,
      attrs: Seq[TagMod] = Seq.empty,
      onPress: (ReactEventI) => Callback
  )

  val component = ReactComponentB[Props]("IconButton").stateless.render_P { $ =>
    <.button(
      ^.`type` := "button",
      Style.defaultButton,
      ^.onClick ==> $.onPress,
      $.attrs,
      <.i(^.className := $.iconCls)
    )
  }.build

  def apply(props: Props) = component(props)

  def apply(
      iconCls: String,
      attrs: Seq[TagMod],
      onPress: (ReactEventI) => Callback
  ) =
    component(Props(iconCls, attrs, onPress))

  def apply(iconCls: String, attrs: Seq[TagMod]) =
    component(Props(iconCls, attrs, { e: ReactEventI =>
      Callback.empty
    }))

}
