package net.scalytica.symbiotic

import japgolly.scalajs.react._
import net.scalytica.symbiotic.css.AppCSS
import net.scalytica.symbiotic.routing.SymbioticRouter
import org.scalajs.dom

object SymbioticApp {

  def main(args: Array[String]): Unit = {
    AppCSS.load()
    SymbioticRouter
      .router()
      .render(dom.document.getElementsByClassName("symbiotic")(0))
  }

}
