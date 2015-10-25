/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic

package object logger {

  private val defaultLogger = LoggerFactory.getLogger("Log")

  def log = defaultLogger

}
