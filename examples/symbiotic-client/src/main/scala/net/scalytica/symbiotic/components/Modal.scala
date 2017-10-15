package net.scalytica.symbiotic.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._

object Modal {

  case class Props(
      id: String,
      header: Option[String] = None,
      ariaLabel: String = "",
      body: TagMod,
      footer: Option[TagMod] = None
  )

  val component = ReactComponentB[Props]("Modal").stateless.render_P { p =>
    <.div(
      ^.id := p.id,
      ^.className := "modal fade",
      ^.tabIndex := "-1",
      ^.role := "dialog",
      "aria-labelledby".reactAttr := p.ariaLabel
    )(
      <.div(^.className := "modal-dialog", ^.role := "document")(
        <.div(^.className := "modal-content")(
          p.header
            .map(
              h =>
                <.div(^.className := "modal-header")(
                  <.button(
                    ^.`type` := "button",
                    ^.className := "close",
                    "data-dismiss".reactAttr := "modal",
                    "aria-label".reactAttr := "Close"
                  )(
                    <.span(
                      "aria-hidden".reactAttr := "true",
                      <.i(^.className := "fa fa-times")
                    )
                  ),
                  <.h4(^.id := s"${p.id}-label", ^.className := "modal-title")(
                    h
                  )
              )
            )
            .getOrElse(EmptyTag),
          <.div(^.className := "modal-body")(p.body),
          p.footer
            .map(ft => <.div(^.className := "modal-footer")(ft))
            .getOrElse(EmptyTag)
        )
      )
    )
  }.build

  def apply(
      id: String,
      header: Option[String] = None,
      ariaLabel: String = "",
      body: TagMod,
      footer: Option[TagMod] = None
  ) = component(Props(id, header, ariaLabel, body, footer))
}
