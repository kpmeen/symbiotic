/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

import scalacss.Defaults._

object FontAwesome extends StyleSheet.Inline {
  import dsl._

  // Base
  val fa = mixin(addClassName("fa"))

  // Misc
  val home         = mixin(fa, addClassName("fa-home"))
  val book         = mixin(fa, addClassName("fa-book"))
  val user         = mixin(fa, addClassName("fa-user"))
  val powerOff     = mixin(fa, addClassName("fa-power-off"))
  val spinner      = mixin(fa, addClassName("fa-spinner"))
  val pulse        = mixin(fa, addClassName("fa-pulse"))
  val chevronLeft  = mixin(fa, addClassName("fa-chevron-left"))
  val chevronRight = mixin(fa, addClassName("fa-chevron-right"))

  // File icons
  val folder     = mixin(fa, addClassName("fa-folder"))
  val folderOpen = mixin(fa, addClassName("fa-folder-open"))
  val file       = mixin(fa, addClassName("fa-file-o"))
  val pdf        = mixin(fa, addClassName("fa-file-pdf-o"))
  val txt        = mixin(fa, addClassName("fa-file-text-o"))
  val image      = mixin(fa, addClassName("fa-file-image-o"))
  val sound      = mixin(fa, addClassName("fa-file-audio-o"))
  val movie      = mixin(fa, addClassName("fa-file-video-o"))
  val archive    = mixin(fa, addClassName("fa-file-archive-o"))
  val word       = mixin(fa, addClassName("fa-file-word-o"))
  val excel      = mixin(fa, addClassName("fa-file-excel-o"))
  val powerpoint = mixin(fa, addClassName("fa-file-powerpoint-o"))
  val hddDrive   = style("hdd-icon")(fa, addClassName("fa-hdd-o"))

  // Social icons
  val google  = style("google-icon")(fa, addClassName("fa-google-plus"))
  val github  = style("github-icon")(fa, addClassName("fa-github"))
  val twitter = style("twitter-icon")(fa, addClassName("fa-twitter"))

  // Sizes
  val sizeLg = styleS(addClassName("fa-lg"))
  val size2x = styleS(addClassName("fa-2x"))
  val size3x = styleS(addClassName("fa-3x"))
  val size4x = styleS(addClassName("fa-4x"))
  val size5x = styleS(addClassName("fa-5x"))

}
