/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

import scalacss.Defaults._

object Colors extends StyleSheet.Inline {
  import dsl._

  sealed trait Color {
    val name: String

    def plain = mixin(addClassName(name))

    def colorLighten(strength: Int) = shade("lighten", strength)

    def colorDarken(strength: Int) = shade("darken", strength)

    def colorAccent(strength: Int) = shade("accent", strength)

    private def shade(shade: String, strength: Int) = {
      assert(strength > 0 && strength <= 5)
      mixin(addClassNames(name, s"$shade-$strength"))
    }
  }

  object DeepPurple extends Color {
    override val name: String = "deep-purple"
  }

}
