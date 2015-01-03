/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.workflow

import models.core.Id
import reactivemongo.bson.BSONObjectID

sealed trait KanbanId extends Id

case class BoardId(id: BSONObjectID) extends KanbanId

case class ColumnId(id: BSONObjectID) extends KanbanId

case class CardId(id: BSONObjectID) extends KanbanId