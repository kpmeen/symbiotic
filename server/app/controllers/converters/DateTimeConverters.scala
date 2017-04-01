/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package controllers.converters

import org.joda.time.DateTime
import play.api.libs.json._

/**
 * Trait for handling conversion to/from DateTime
 */
trait DateTimeConverters {

  val defaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String  = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter = Format[DateTime](
    Reads
      .jodaDateReads(defaultReadDateTimePattern)
      .orElse(Reads.jodaDateReads(readDateTimeMillisPattern)),
    Writes.jodaDateWrites(defaultReadDateTimePattern)
  )

}

object DateTimeConverters extends DateTimeConverters
