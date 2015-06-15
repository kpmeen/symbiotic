package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components.LeftNav
import net.scalytica.symbiotic.components.TopNav.Style
import net.scalytica.symbiotic.pages.{LoginPage, HomePage, ItemsPage}
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      Style,
      LeftNav.Style,
      ItemsPage.Style,
      HomePage.Style,
      LoginPage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
