package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components.{Spinner, TopNav}
import net.scalytica.symbiotic.components.dman.{FileInfo, FolderContent, PathCrumb}
import net.scalytica.symbiotic.pages.{DocManagementPage, HomePage, LoginPage}

import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry
import scalacss.Defaults._

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      FontAwesome,
      HomePage.Style,
      Spinner.Style,
//      Footer.Style,
      TopNav.Style,
      LoginPage.Style,
      DocManagementPage.Style,
//      FolderTree.Style,
//      FolderTreeItem.Style,
      FolderContent.Style,
//      PathCrumb.Style,
      FileInfo.Style,
      FileTypes.Styles
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
