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
trait ModalOptions extends js.Object {
  var backdrop: Boolean = js.native
  var keyboard: Boolean = js.native
  var show: Boolean = js.native
  var remote: String = js.native
}

@js.native
trait ModalOptionsBackdropString extends js.Object {
  var backdrop: String = js.native
  var keyboard: Boolean = js.native
  var show: Boolean = js.native
  var remote: String = js.native
}

@js.native
trait TooltipOptions extends js.Object {
  var animation: Boolean = js.native
  var html: Boolean = js.native
  var placement: js.Any = js.native
  var selector: String = js.native
  var title: js.Any = js.native
  var trigger: String = js.native
  var delay: js.Any = js.native
  var container: js.Any = js.native
}

@js.native
trait PopoverOptions extends js.Object {
  var animation: Boolean = js.native
  var html: Boolean = js.native
  var placement: js.Any = js.native
  var selector: String = js.native
  var trigger: String = js.native
  var title: js.Any = js.native
  var content: js.Any = js.native
  var delay: js.Any = js.native
  var container: js.Any = js.native
}

@js.native
trait ScrollSpyOptions extends js.Object {
  var offset: Double = js.native
}

@js.native
trait CollapseOptions extends js.Object {
  var parent: js.Any = js.native
  var toggle: Boolean = js.native
}

@js.native
trait CarouselOptions extends js.Object {
  var interval: Double = js.native
  var pause: String = js.native
}

@js.native
trait TypeaheadOptions extends js.Object {
  var source: js.Any = js.native
  var items: Double = js.native
  var minLength: Double = js.native
  var matcher: js.Function1[js.Any, Boolean] = js.native
  var sorter: js.Function1[js.Array[js.Any], js.Array[js.Any]] = js.native
  var updater: js.Function1[js.Any, Any] = js.native
  var highlighter: js.Function1[js.Any, String] = js.native
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