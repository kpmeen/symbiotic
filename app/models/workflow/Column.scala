/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.workflow

import play.api.libs.json.Json


/**
 * Represents a Column as applied on a Kanban board
 */
case class Column(_id: ColumnId, name: String, description: Option[String])

object Column {

  implicit val columnReads = Json.reads[Column]
  implicit val columnWrites = Json.writes[Column]

}