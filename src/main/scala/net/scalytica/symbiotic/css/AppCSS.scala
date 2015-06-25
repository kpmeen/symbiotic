package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components.dman.{PathCrumb, FolderContent, FolderNode, DocBrowser}
import net.scalytica.symbiotic.components.{LeftNav, TopNav}
import net.scalytica.symbiotic.pages.{DocManagementPage, LoginPage, HomePage, ItemsPage}
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      Colors,
      TopNav.Style,
      LeftNav.Style,
      ItemsPage.Style,
      HomePage.Style,
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
