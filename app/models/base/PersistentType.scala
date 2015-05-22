/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.base

import org.bson.types.ObjectId
import play.api.libs.json._

trait PersistentType {

  val _id: Option[ObjectId]
}

trait PersistentTypeConverters {
  implicit val reads: Reads[ObjectId] = __.read[String].map(s => new ObjectId(s))
  implicit val writes: Writes[ObjectId] = Writes {
    (a: ObjectId) => JsString(a.toString)
  }
}
