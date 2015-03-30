/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core

import com.mongodb.DBObject
import core.converters.{WithBSONConverters, WithDateTimeConverters}
import core.mongodb.WithMongo
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * This case class holds actual process configuration.
 *
 * @param id ProcessId The unique identifier for the Process
 * @param name String with a readable name
 * @param strict Boolean flag indicating if movement of tasks in the process should be free-form/open or restricted
 * @param description String Readable text describing the process
 * @param steps List of Steps in the process.
 */
case class Process(
  id: Option[ProcessId],
  name: String,
  strict: Boolean = false,
  description: Option[String] = None,
  steps: List[Step] = List.empty)

object Process extends WithBSONConverters[Process] with WithDateTimeConverters with WithMongo {

  implicit val procFormat: Format[Process] = (
    (__ \ "id").formatNullable[ProcessId] and
      (__ \ "name").format[String] and
      (__ \ "strict").format[Boolean] and
      (__ \ "description").formatNullable[String] and
      (__ \ "steps").format[List[Step]]
    )(Process.apply, unlift(Process.unapply))

  override implicit def toBSON(x: Process): DBObject = ???


  override implicit def fromBSON(dbo: DBObject): Process = ???

  override val collectionName: String = "processes"

  // TODO: Implement persistence here....
}