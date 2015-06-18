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

    def plain = mixin(addClassName(name))

    def colorLighten(strength: Int) = {
      assert(strength > 0 && strength <= 5)
      shade("lighten", strength)
    }

    def colorDarken(strength: Int) = {
      assert(strength > 0 && strength <= 4)
      shade("darken", strength)
    }

    def colorAccent(strength: Int) = {
      assert(strength > 0 && strength <= 5)
      shade("accent", strength)
    }

    private def shade(shade: String, strength: Int) = mixin(addClassNames(name, s"$shade-$strength"))
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

  val Black = "#000000".color
  val White = "#FFFFFF".color

  val TopMenuColor = DeepPurple.colorLighten(1)
  val TopMenuItemSelected = DeepPurple.colorDarken(1)
  val TopMenuItemHover = backgroundColor("#9575cd".color)
  val LeftMenuItemSelected = DeepPurple.colorLighten(3)
  val LeftMenuItemHover = backgroundColor("#ede7f6".color)
  val FooterColor = DeepPurple.colorLighten(4)

}
