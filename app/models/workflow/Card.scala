/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.workflow

import play.api.libs.json.Json

/**
 * The interesting bit...a card is the task moved around the board through the different columns.
 */
case class Card(
  id: CardId = CardId(),
  boardId: BoardId,
  columnId: ColumnId,
  title: String,
  description: Option[String]) {

  /**
   * This function allows for moving a card around the board. If on a strict board the movement will be restricted to
   * the previous and next columns. If it is open, the card can be moved anywhere.
   *
   * @param newColumnId The new ColumnId to move to
   * @param findBoard function that returns a Board based on BoardId
   * @return An option of Card. Will be None if the move was restricted.
   */
  def move(newColumnId: ColumnId)(findBoard: (BoardId) => Option[Board]): Option[Card] = {
    findBoard(boardId).flatMap { b =>
      if (b.strict) {
        b.prevNextColumns(columnId) match {
          case PrevNextCol(prev, next) if newColumnId == prev || newColumnId == next => Some(this.copy(columnId = newColumnId))
          case PrevOnlyCol(prev) if newColumnId == prev => Some(this.copy(columnId = newColumnId))
          case NextOnlyCol(next) if newColumnId == next => Some(this.copy(columnId = newColumnId))
          case _ => None
        }
      } else {
        Some(this.copy(columnId = newColumnId))
      }
    }
  }

}

object Card {

  implicit val cardReads = Json.reads[Card]
  implicit val cardWrites = Json.writes[Card]

  /**
   * Adds a new Card to the board in the left-most column.
   *
   * @param title the title of the Card to add
   * @param desc the description of the Card to add
   * @return an Option[Card]
   */
  def addToBoard(board: Board, title: String, desc: Option[String]): Option[Card] =
    board.columns.headOption.flatMap(col => Some(
      Card(
        boardId = board._id,
        columnId = col._id,
        title = title,
        description = desc
      )
    ))

}
