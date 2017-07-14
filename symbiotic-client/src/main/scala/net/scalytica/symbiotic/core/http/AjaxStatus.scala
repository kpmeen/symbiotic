package net.scalytica.symbiotic.core.http

sealed trait AjaxStatus

case object Loading            extends AjaxStatus
case object Finished           extends AjaxStatus
case class Failed(msg: String) extends AjaxStatus
