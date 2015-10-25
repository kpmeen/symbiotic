/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.logger

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Facade for functions in log4javascript that we need
 */
@js.native
private[logger] trait Log4JavaScript extends js.Object {
  def getLogger(name: js.UndefOr[String]): JSLogger = js.native

  def setEnabled(enabled: Boolean): Unit = js.native

  def isEnabled: Boolean = js.native
}

@JSName("log4javascript.Level")
@js.native
private[logger] trait Level extends js.Object {
  val ALL: Level = js.native
  val TRACE: Level = js.native
  val DEBUG: Level = js.native
  val INFO: Level = js.native
  val WARN: Level = js.native
  val ERROR: Level = js.native
  val FATAL: Level = js.native
}

@JSName("log4javascript.Logger")
@js.native
private[logger] trait JSLogger extends js.Object {
  def addAppender(appender: Appender): Unit = js.native

  def removeAppender(appender: Appender): Unit = js.native

  def removeAllAppenders(appender: Appender): Unit = js.native

  def setLevel(level: Level): Unit = js.native

  def getLevel: Level = js.native

  def trace(msg: Any, error: js.UndefOr[js.Error]): Unit = js.native

  def debug(msg: Any, error: js.UndefOr[js.Error]): Unit = js.native

  def info(msg: Any, error: js.UndefOr[js.Error]): Unit = js.native

  def warn(msg: Any, error: js.UndefOr[js.Error]): Unit = js.native

  def error(msg: Any, error: js.UndefOr[js.Error]): Unit = js.native

  def fatal(msg: Any, error: js.UndefOr[js.Error]): Unit = js.native

  def trace(msg: Any): Unit = js.native

  def debug(msg: Any): Unit = js.native

  def info(msg: Any): Unit = js.native

  def warn(msg: Any): Unit = js.native

  def error(msg: Any): Unit = js.native

  def fatal(msg: Any): Unit = js.native
}

@JSName("log4javascript.Layout")
@js.native
private[logger] trait Layout extends js.Object

@JSName("log4javascript.JsonLayout")
@js.native
private[logger] class JsonLayout extends Layout

@JSName("log4javascript.Appender")
@js.native
private[logger] trait Appender extends js.Object {
  def setLayout(layout: Layout): Unit = js.native

  def setThreshold(level: Level): Unit = js.native
}

@JSName("log4javascript.BrowserConsoleAppender")
@js.native
private[logger] class BrowserConsoleAppender extends Appender

@JSName("log4javascript.PopUpAppender")
@js.native
private[logger] class PopUpAppender extends Appender

@JSName("log4javascript.AjaxAppender")
@js.native
private[logger] class AjaxAppender(url: String) extends Appender {
  def addHeader(header: String, value: String): Unit = js.native
}

@js.native
private[logger] object Log4JavaScript extends js.GlobalScope {
  val log4javascript: Log4JavaScript = js.native
}

class L4JSLogger(jsLogger: JSLogger) extends Logger {

  private var ajaxAppender: AjaxAppender = null

  private def undefOrError(e: Exception): js.UndefOr[js.Error] = {
    if (e == null)
      js.undefined
    else
      e.asInstanceOf[js.Error]
  }

  override def trace(msg: Any, e: Exception): Unit = jsLogger.trace(msg, undefOrError(e))

  override def trace(msg: Any): Unit = jsLogger.trace(msg)

  override def debug(msg: Any, e: Exception): Unit = jsLogger.debug(msg, undefOrError(e))

  override def debug(msg: Any): Unit = jsLogger.debug(msg)

  override def info(msg: Any, e: Exception): Unit = jsLogger.info(msg, undefOrError(e))

  override def info(msg: Any): Unit = jsLogger.info(msg)

  override def warn(msg: Any, e: Exception): Unit = jsLogger.warn(msg, undefOrError(e))

  override def warn(msg: Any): Unit = jsLogger.warn(msg)

  override def error(msg: Any, e: Exception): Unit = jsLogger.error(msg, undefOrError(e))

  override def error(msg: Any): Unit = jsLogger.error(msg)

  override def fatal(msg: Any, e: Exception): Unit = jsLogger.fatal(msg, undefOrError(e))

  override def fatal(msg: Any): Unit = jsLogger.fatal(msg)

  override def enableServerLogging(url: String): Unit = {
    if (ajaxAppender == null) {
      ajaxAppender = new AjaxAppender(url)
      ajaxAppender.addHeader("Content-Type", "application/json")
      ajaxAppender.setLayout(new JsonLayout)
      jsLogger.addAppender(ajaxAppender)

    }
  }

  override def disableServerLogging(): Unit = {
    if (ajaxAppender != null) {
      jsLogger.removeAppender(ajaxAppender)
      ajaxAppender = null
    }
  }
}