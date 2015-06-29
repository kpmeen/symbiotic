/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css


import scalacss.Defaults._

object FontAwesome extends StyleSheet.Inline {
  import dsl._

  val folder = mixin(addClassNames("fa", "fa-folder"))

  val folderOpen = mixin(addClassNames("fa", "fa-folder-open"))

  val file = mixin(addClassNames("fa", "fa-file"))

  val hddDrive = style(addClassNames("fa", "fa-hdd-o"))


}
