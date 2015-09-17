package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components.dman._
import net.scalytica.symbiotic.components.{Footer, Spinner, TopNav}
import net.scalytica.symbiotic.pages.{DocManagementPage, HomePage, LoginPage}

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      Material,
      MaterialColors,
      FontAwesome,
      HomePage.Style,
      Spinner.Style,
      Footer.Style,
      TopNav.Style,
      LoginPage.Style,
      DocManagementPage.Style,
      FolderTree.Style,
      FolderTreeItem.Style,
      FolderContent.Style,
      PathCrumb.Style,
      FileInfo.Style,
      FileTypes.Styles
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
