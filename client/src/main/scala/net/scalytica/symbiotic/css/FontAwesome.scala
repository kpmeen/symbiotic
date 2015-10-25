/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css


import scalacss.Defaults._

object FontAwesome extends StyleSheet.Inline {
  import dsl._

  val folder = mixin(addClassNames("fa", "fa-folder"))

  val folderOpen = mixin(addClassNames("fa", "fa-folder-open"))

  val file = mixin(addClassNames("fa", "fa-file-o"))

  val pdf = mixin(addClassNames("fa", "fa-file-pdf-o"))

  val txt = mixin(addClassNames("fa", "fa-file-text-o"))

  val image = mixin(addClassNames("fa", "fa-file-image-o"))

  val sound = mixin(addClassNames("fa", "fa-file-audio-o"))

  val movie = mixin(addClassNames("fa", "fa-file-video-o"))

  val archive = mixin(addClassNames("fa", "fa-file-archive-o"))

  val word = mixin(addClassNames("fa", "fa-file-word-o"))

  val excel = mixin(addClassNames("fa", "fa-file-excel-o"))

  val powerpoint = mixin(addClassNames("fa", "fa-file-powerpoint-o"))


  val hddDrive = style(addClassNames("fa", "fa-hdd-o"))


}
