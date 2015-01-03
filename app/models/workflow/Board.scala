/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.workflow

import models.workflow.Board._
import reactivemongo.bson.BSONObjectID

case class Board(id: BoardId,
  name: String,
  strict: Boolean = false,
  description: Option[String],
  columns: List[Column] = List.empty[Column]) {

  /**
   * Appends a column to the Board.
   */
  def appendColumn(col: Column): Board = this.copy(columns = columns ::: List(col))

  /**
   * Inserts a column on the board at the defined index. If the index is larger than the current number of columns, the
   * Column is appended to the board. If not it will be added at the given index, shifting tailing columns to the right.
   *
   * @param col the Column to insert
   * @param index the position to insert the column in the list of columns
   * @return a Board with the new Column added to the list of columns
   */
  def insertColumn(col: Column, index: Int): Board = {
    if (index > columns.length) {
      appendColumn(col)
    } else {
      val lr = columns.splitAt(index)
      this.copy(columns = lr._1 ::: List(col) ::: lr._2)
    }
  }

  /**
   * Allows for re-arranging columns on the board...
   *
   * currIndex > newIndex: the column is moved `before` its current location
   * currIndex < newIndex: the column is moved `after` its current location
   * currIndex == newIndex: the columns are left alone
   *
   * TODO: Should it be allowed to rearrange Columns with Cards??? Hmmm....
   *
   * @param currIndex the current index position of the column
   * @param newIndex the new index position to place the column
   * @return A Board with an updated column order
   */
  def moveColumn(currIndex: Int, newIndex: Int): Board = {
    if (currIndex == newIndex) {
      this
    } else {
      val index = if (newIndex >= columns.length) columns.length - 1 else newIndex
      val lr = columns.splitAt(currIndex)
      val removed = (lr._1 ::: lr._2.tail).splitAt(index)

      this.copy(columns = removed._1 ::: List(columns(currIndex)) ::: removed._2)
    }
  }

  /**
   * Removes the column at the given index if the index number is lower or equal to the number of columns, and ff the
   * column to be removed does not contain any Cards.
   *
   * @param columnIndex the column index to remove
   * @param findCards function to identify which cards belong to the given columnId on the given boardId.
   * @return Some[Board] if the column was removed, otherwise None
   */
  def removeColumn(columnIndex: Int)(findCards: (BoardId, ColumnId) => List[Card]): Option[Board] = {
    if (columnIndex < columns.length) {
      if (columns.isDefinedAt(columnIndex)) {
        val cards = findCards(id, columns(columnIndex).id)
        if (cards.isEmpty) {
          val lr = columns.splitAt(columnIndex)
          return Some(this.copy(columns = lr._1 ::: lr._2.tail))
        }
      }
    }
    None
  }

  /**
   * Calculates the current surroundings for the current Column
   *
   * @param currColId the current ColumnId
   * @return a type of PrevNextColType that may or may not have previous and/or next column references.
   */
  private[workflow] def prevNextColumns(currColId: ColumnId): PrevNextColType = {
    val currIndex = columns.indexWhere(_.id == currColId)

    if (currIndex == 0) {
      NextOnlyCol(columns(1).id)
    } else if (currIndex == columns.length - 1) {
      PrevOnlyCol(columns(columns.length - 2).id)
    } else {
      PrevNextCol(columns(currIndex - 1).id, columns(currIndex + 1).id)
    }
  }
}

object Board {

  def create(name: String, strict: Boolean = false, desc: Option[String]): Board =
    Board(
      id = BoardId(BSONObjectID.generate),
      name = name,
      strict = strict,
      description = desc
    )

  /**
   * Types indicating which columns are surrounding the current Column.
   */
  private[workflow] sealed trait PrevNextColType

  private[workflow] case class PrevNextCol(prev: ColumnId, next: ColumnId) extends PrevNextColType

  private[workflow] case class PrevOnlyCol(prev: ColumnId) extends PrevNextColType

  private[workflow] case class NextOnlyCol(next: ColumnId) extends PrevNextColType
}

/**
 * Represents a Column as applied on a Kanban board
 */
case class Column(id: ColumnId, name: String, description: Option[String])