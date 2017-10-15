package net.scalytica.symbiotic

package object logger {

  private val defaultLogger = LoggerFactory.getLogger("Log")

  def log = defaultLogger

}
