/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.routing

import java.util.UUID

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import net.scalytica.symbiotic.models.FileId
import net.scalytica.symbiotic.models.dman.FolderItem
import net.scalytica.symbiotic.pages.DocManagementPage

object DMan {

  case class FolderURIElem(selectedFolderId: Option[UUID] = None)

  object FolderURIElem {

    implicit def fromFolderItem(fi: FolderItem): FolderURIElem = FolderURIElem(Option(fi.folderId.toUUID))

  }

  val routes = RouterConfigDsl[FolderURIElem].buildRule { dsl =>
    import dsl._

    dynamicRouteCT(
      uuid.option.caseClass[FolderURIElem]
    ) ~> dynRenderR((furie, ctl) =>
      DocManagementPage(
        selectedFolder = furie.selectedFolderId.map(id => FileId(id.toString)),
        selectedFile = None,
        ctl = ctl
      )
    )
  }

}
