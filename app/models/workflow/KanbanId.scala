/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.workflow

import com.mongodb.casbah.TypeImports.ObjectId
import models.core.{Id, WithIdTransformers}
import play.api.libs.json._

sealed trait KanbanId extends Id

case class BoardId(id: ObjectId) extends KanbanId

case class ColumnId(id: ObjectId = new ObjectId) extends KanbanId

case class CardId(id: ObjectId = new ObjectId) extends KanbanId

object BoardId extends WithIdTransformers {
  implicit val boardIdReads: Reads[BoardId] = reads[BoardId](BoardId.apply)
  implicit val boardIdWrites: Writes[BoardId] = writes[BoardId]
}

object ColumnId extends WithIdTransformers {
  implicit val columnIdReads: Reads[ColumnId] = reads[ColumnId](ColumnId.apply)
  implicit val columnIdWrites: Writes[ColumnId] = writes[ColumnId]
}

object CardId extends WithIdTransformers {
  implicit val cardIdReads: Reads[CardId] = reads[CardId](CardId.apply)
  implicit val cardIdWrites: Writes[CardId] = writes[CardId]
}