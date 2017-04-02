/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman.foldercontent

import scalacss.Defaults._

object FolderContentStyle extends StyleSheet.Inline {

  import dsl._

  val loading = style("filecontent-loading")(
    addClassNames("center-block", "text-center"),
    height(100 %%),
    width(100 %%)
  )

  val contentPanel = style("content-panel")(
    addClassNames("panel", "panel-default"),
    marginTop(10 px),
    boxShadow := "none",
    borderRadius `0`,
    border `0`
  )

  val contentPanelBody = style("content-panel-body")(
    addClassNames("panel-body"),
    padding `0`
  )

  val folder = styleF.bool(showAsBlock => styleS(
    color steelblue,
    mixinIfElse(showAsBlock)(display block)(display inlineBlock)
  ))

  val file = styleF.bool(showAsBlock => styleS(
    color lightslategrey,
    mixinIfElse(showAsBlock)(display block)(display inlineBlock)
  ))
}
