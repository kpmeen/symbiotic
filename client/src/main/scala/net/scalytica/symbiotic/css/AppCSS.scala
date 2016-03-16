package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components._
import net.scalytica.symbiotic.components.dman._
import net.scalytica.symbiotic.components.dman.foldercontent.{TableView, FolderContentStyle, IconView}
import net.scalytica.symbiotic.components.dman.foldertree.{FolderTreeItem, FolderTree}
import net.scalytica.symbiotic.pages._

import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry
// (Not so)IntelliJ thinks this import is not used...well...it's wrong. Very wrong!
import scalacss.Defaults._

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      LoginStyle,
      AuthCallbackPage.Style,
      FontAwesome,
      Spinner.Style,
      FileInfo.Style,
      FileTypes.Styles,
      HomePage.Style,
      TopNav.Style,
      Footer.Style,
      UserProfilePage.Style,
      DocManagementPage.Style,
      FolderTree.Style,
      FolderTreeItem.Style,
      FolderContentStyle,
      IconView.Style,
      TableView.Style,
      PathCrumb.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
