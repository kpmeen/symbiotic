package net.scalytica.symbiotic

import japgolly.scalajs.react._
import net.scalytica.symbiotic.css.AppCSS
import net.scalytica.symbiotic.routes.AppRouter
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object SymbioticApp extends JSApp {
  @JSExport
  override def main(): Unit = {
    AppCSS.load
    AppRouter.router().render(dom.document.body)
  }

}

