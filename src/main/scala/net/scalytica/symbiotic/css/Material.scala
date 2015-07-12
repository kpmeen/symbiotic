/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

import scalacss.Defaults._

object Material extends StyleSheet.Inline {

  import dsl._

  // TODO: Should be possible to create proper wrappers for the materializecss styles.

  val container = style(addClassNames("container"))

  val cardDefault = style(addClassName("card"))
  val cardMedium = style(className = "cardMedium")(addClassNames("card", "medium"))
  val cardContent = style(addClassNames("card-content"))
  val cardTitle = style(
    addClassNames("card-title"),
    color(MaterialColors.GreyDarken4)
  )

  val row = style(addClassNames("row"))
  val col = mixin(addClassNames("col"))

  val valignWrapper = mixin(addClassName("valign-wrapper"))
  val centerAlign = mixin(addClassNames("center-align"))

  val truncate = mixin(addClassName("truncate"))

  val inputField = mixin(addClassName("input-field"))

}
