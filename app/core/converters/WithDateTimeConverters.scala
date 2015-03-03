/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.converters

import org.joda.time.DateTime
import play.api.libs.json.{Format, Reads, Writes}

/**
 * Trait for handling conversion to/from DateTime
 */
trait WithDateTimeConverters {

  val DefaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val ReadDateTimeMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter = Format[DateTime](
    Reads.jodaDateReads(DefaultReadDateTimePattern).orElse(Reads.jodaDateReads(ReadDateTimeMillisPattern)),
    Writes.jodaDateWrites(DefaultReadDateTimePattern)
  )

  implicit def asDateTime(jud: java.util.Date): DateTime = new DateTime(jud)

  implicit def asOptDateTime(maybeJud: Option[java.util.Date]): Option[DateTime] = maybeJud.flatMap(jud => Option(asDateTime(jud)))

}
