/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.routing

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import net.scalytica.symbiotic.pages.DocManagementPage

object DMan {

  case class FolderPath(selectedFolder: Option[String] = None)

  val routes = RouterConfigDsl[FolderPath].buildRule { dsl =>
    import dsl._

    dynamicRouteCT(
      ("library" ~ string(".*$").option).caseClass[FolderPath]
    ) ~> dynRenderR((fp, r) => DocManagementPage(fp.selectedFolder, r))
  }

}
