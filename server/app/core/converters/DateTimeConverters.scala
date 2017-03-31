/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.converters

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

  implicit def asDateTime(jud: java.util.Date): DateTime = new DateTime(jud)

  implicit def asOptDateTime(
      maybeJud: Option[java.util.Date]
  ): Option[DateTime] =
    maybeJud.map(jud => asDateTime(jud))

}

object DateTimeConverters extends DateTimeConverters
