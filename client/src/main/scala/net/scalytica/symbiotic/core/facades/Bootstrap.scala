/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.core.facades

import org.scalajs.jquery.JQuery

import scala.scalajs.js

object Bootstrap {
  implicit def jquery2Bootstrap(jquery: JQuery): Bootstrap =
    jquery.asInstanceOf[Bootstrap]
}

@js.native
trait Bootstrap extends JQuery {

  def affix(options: Option[AffixOptions] = None): this.type = js.native

}

@js.native
trait AffixOptions extends js.Object {
  var offset: Int = js.native
  var target: js.Any = js.native
}

@js.native
trait BootstrapAffix extends AffixOptions {
  var affix: JQuery = js.native
}