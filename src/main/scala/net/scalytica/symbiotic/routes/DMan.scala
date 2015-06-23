/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.routes

import java.util.UUID

import japgolly.scalajs.react.extra.router2.RouterConfigDsl
import net.scalytica.symbiotic.pages.DocManagementPage

object DMan {

  case class FolderPath(
    cid: UUID,
    selectedFolder: Option[String] = None)

  val routes = RouterConfigDsl[FolderPath].buildRule { dsl =>
    import dsl._

    dynamicRouteCT((uuid ~ ("/root" ~ string(".*$").option)).caseclass2[UUID, Option[String], FolderPath] {
      (cid, sf) => FolderPath(cid, sf)
    }(FolderPath.unapply)
    ) ~> dynRenderR((fp, r) => DocManagementPage(fp.cid.toString, "", fp.selectedFolder, r))
  }

}
