/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.casbah.commons.Imports._
import core.converters.ObjectBSONConverters
import hipe.core.DurationUnits._
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait DurationUnit {
  val strVal: String
}

object DurationUnits {

  case object Seconds extends DurationUnit {
    override val strVal: String = "seconds"
  }

  case object Minutes extends DurationUnit {
    override val strVal: String = "minutes"
  }

  case object Hours extends DurationUnit {
    override val strVal: String = "hours"
  }

  case object Days extends DurationUnit {
    override val strVal: String = "days"
  }

  case object Weeks extends DurationUnit {
    override val strVal: String = "weeks"
  }

  case object Months extends DurationUnit {
    override val strVal: String = "months"
  }

  case object Years extends DurationUnit {
    override val strVal: String = "years"
  }

}

object DurationUnit {
  implicit val reads: Reads[DurationUnit] = Reads[DurationUnit] { case value: JsValue =>
    val jsv = value.as[String]
    fromString(jsv).map(du => JsSuccess(du)).getOrElse(JsError(s"Invalid duration unit $jsv."))
  }

  implicit val writes: Writes[DurationUnit] = Writes {
    case value: DurationUnit => JsString(value.strVal)
  }

  def fromString(str: String): Option[DurationUnit] = {
    str match {
      case Seconds.strVal => Some(Seconds)
      case Minutes.strVal => Some(Minutes)
      case Hours.strVal => Some(Hours)
      case Days.strVal => Some(Days)
      case Weeks.strVal => Some(Weeks)
      case Months.strVal => Some(Months)
      case Years.strVal => Some(Years)
      case _ => None
    }
  }
}

case class Duration(num: Int, unit: DurationUnit) {

  def toDateTimeFromNow: DateTime = unit match {
    case Seconds => DateTime.now().plusSeconds(num)
    case Minutes => DateTime.now().plusMinutes(num)
    case Hours => DateTime.now().plusHours(num)
    case Days => DateTime.now().plusDays(num)
    case Weeks => DateTime.now().plusWeeks(num)
    case Months => DateTime.now().plusMonths(num)
    case Years => DateTime.now().plusYears(num)
  }

}

object Duration extends ObjectBSONConverters[Duration] {
  implicit val format: Format[Duration] = Json.format[Duration]

  override def toBSON(x: Duration): DBObject = MongoDBObject("num" -> x.num, "unit" -> x.unit.strVal)

  override def fromBSON(dbo: DBObject): Duration = {
    Duration(
      num = dbo.as[Int]("num"),
      unit = DurationUnit.fromString(dbo.as[String]("unit")).getOrElse {
        throw new IllegalArgumentException("Illegal duration unit")
      }
    )
  }
}
