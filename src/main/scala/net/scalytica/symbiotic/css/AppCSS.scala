package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components.dman.{DocBrowser, FolderContent, FolderNode, PathCrumb}
import net.scalytica.symbiotic.components.{Spinner, Footer, LeftNav, TopNav}
import net.scalytica.symbiotic.pages.{DocManagementPage, ItemsPage, LoginPage}

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      Colors,
      Material,
      FontAwesome,
      Spinner.Style,
      Footer.Style,
      TopNav.Style,
      LeftNav.Style,
      ItemsPage.Style,
      LoginPage.Style,
      DocManagementPage.Style,
      DocBrowser.Style,
      FolderNode.Style,
      FolderContent.Style,
      PathCrumb.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
