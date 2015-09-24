package net.scalytica.symbiotic.models

import japgolly.scalajs.react.vdom.ReactTag
import net.scalytica.symbiotic.routes.SymbioticRouter.View

case class Menu(name: String, route: View, tag: Option[ReactTag])