package net.scalytica.symbiotic.time

import org.joda.time.{DateTime => JodaDateTime, DateTimeZone}

object SymbioticDateTime {

  def now: JodaDateTime = JodaDateTime.now(DateTimeZone.UTC)

}
