package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components.dman._
import net.scalytica.symbiotic.components.{Spinner, Footer, LeftNav, TopNav}
import net.scalytica.symbiotic.pages.{HomePage, DocManagementPage, ItemsPage, LoginPage}

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
      HomePage.Style,
      Spinner.Style,
      Footer.Style,
      TopNav.Style,
      LeftNav.Style,
      ItemsPage.Style,
      LoginPage.Style,
      DocManagementPage.Style,
      FolderTree.Style,
      FolderTreeItem.Style,
      FolderContent.Style,
      PathCrumb.Style,
      FileInfo.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
