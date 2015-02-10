/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.core.mapping

import org.joda.time.DateTime
import play.api.libs.json.{Format, Reads, Writes}

/**
 * Trait for handling conversion to/from DateTime
 */
trait WithDateTimeMapping {

  val DefaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val ReadDateTimeMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter = Format[DateTime](
    Reads.jodaDateReads(DefaultReadDateTimePattern).orElse(Reads.jodaDateReads(ReadDateTimeMillisPattern)),
    Writes.jodaDateWrites(DefaultReadDateTimePattern)
  )

}
