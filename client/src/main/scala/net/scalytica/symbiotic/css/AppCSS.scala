package net.scalytica.symbiotic.css

import net.scalytica.symbiotic.components._
import net.scalytica.symbiotic.components.dman._
import net.scalytica.symbiotic.components.dman.foldercontent.{TableView, FolderContentStyle, IconView}
import net.scalytica.symbiotic.pages._

import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry
// (Not so)IntelliJ thinks this import is not used...well...it's wrong. Very wrong!
import scalacss.Defaults._

object AppCSS {

  def load() = {
    GlobalRegistry.register(
      GlobalStyle,
      TopNav.Style,
      Footer.Style,
      FontAwesome,
      HomePage.Style,
      UserProfilePage.Style,
      Spinner.Style,
      LoginPage.Style,
      DocManagementPage.Style,
      FolderTree.Style,
      FolderTreeItem.Style,
      FolderContentStyle,
      IconView.Style,
      TableView.Style,
      PathCrumb.Style,
      FileInfo.Style,
      FileTypes.Styles
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
