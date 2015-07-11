/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

import scalacss.Defaults._

/**
 * For a full list of available colors please refer to the
 * Matierialize CSS documentation: http://materializecss.com/color.html
 */
object Colors extends StyleSheet.Inline {

  import dsl._

  sealed trait MaterializeColor {
    val name: String

    private def shade(shade: String, strength: Int) = styleS(addClassNames(name, s"$shade-$strength"))

    def plain = mixin(addClassName(name))

    def text = mixin(addClassName(s"$name-text"))

  }

  object DeepPurple extends MaterializeColor {
    override val name: String = "deep-purple"
  }

  object Indigo extends MaterializeColor {
    override val name: String = "indigo"
  }

  object Blue extends MaterializeColor {
    override val name: String = "blue"
  }

  object Red extends MaterializeColor {
    override val name: String = "red"
  }

  object Green extends MaterializeColor {
    override val name: String = "green"
  }

  object Grey extends MaterializeColor {
    override val name: String = "grey"
  }

  val colorLighten = styleF(Domain.ofRange(1 to 5))(strength => addClassName(s"lighten-$strength"))
  val colorDarken = styleF(Domain.ofRange(1 to 4))(strength => addClassName(s"darken-$strength"))
  val colorAccent = styleF(Domain.ofRange(1 to 5))(strength => addClassName(s"accent-$strength"))

  val textLighten = styleF(Domain.ofRange(1 to 5))(strength => addClassName(s"text-lighten-$strength"))
  val textDarken = styleF(Domain.ofRange(1 to 4))(strength => addClassName(s"text-darken-$strength"))
  val textAccent = styleF(Domain.ofRange(1 to 5))(strength => addClassName(s"text-accent-$strength"))

  val Black = c"#000000"
  val White = c"#FFFFFF"

  val TopMenuColor = styleS(Indigo.plain, colorLighten(1))
  val TopMenuItemSelected = styleS(Indigo.plain, colorDarken(1))
  val TopMenuItemHover = backgroundColor(c"#7986cb")
  val LeftMenuItemSelected = styleS(Indigo.plain, colorLighten(3))
  val LeftMenuItemHover = backgroundColor(c"#e8eaf6")
  val FooterColor = styleS(Indigo.plain, colorLighten(3))
  val PathCrumbColor = c"#7986cb"

}
