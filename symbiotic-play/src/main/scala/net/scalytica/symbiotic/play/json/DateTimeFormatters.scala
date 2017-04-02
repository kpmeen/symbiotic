package net.scalytica.symbiotic.play.json

import org.joda.time.DateTime
import play.api.libs.json._

/**
 * Trait for handling conversion to/from DateTime
 */
trait DateTimeFormatters {

  val defaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String  = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter: Format[DateTime] = Format[DateTime](
    Reads
      .jodaDateReads(defaultReadDateTimePattern)
      .orElse(Reads.jodaDateReads(readDateTimeMillisPattern)),
    Writes.jodaDateWrites(defaultReadDateTimePattern)
  )

}

object DateTimeFormatters extends DateTimeFormatters
