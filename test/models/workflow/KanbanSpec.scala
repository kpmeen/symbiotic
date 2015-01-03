/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.workflow

import models.workflow.Boards._
import models.workflow.Columns._
import org.scalatest._
import reactivemongo.bson.BSONObjectID

class KanbanSpec extends WordSpec with MustMatchers {

  "A Board" should {
    var board = Board(id = BoardId(BSONObjectID.generate), name = "Test Board", description = Some("Just for testing purposes"))

    "be initialized with no columns" in {
      assert(board.columns.isEmpty)
    }

    "add a column to the empty columns list" in {
      board = board.appendColumn(col0)
      assert(board.columns.length == 1)
    }
    "append a column to the column list" in {
      board = board.appendColumn(col2)
      assert(board.columns.length == 2)
    }
    "insert a column after the first column" in {
      board = board.insertColumn(col1, 1)
      assert(board.columns.length == 3)
      assertResult(board.columns.head.id)(col0.id)
      assertResult(board.columns.last.id)(col2.id)
      assertResult(board.columns(1).id)(col1.id)
    }
    "insert a column before the second to last column" in {
      board = board.insertColumn(col3, 2)
      assert(board.columns.length == 4)
      assertResult(board.columns.head.id)(col0.id)
      assertResult(board.columns(1).id)(col1.id)
      assertResult(board.columns(2).id)(col3.id)
      assertResult(board.columns.last.id)(col2.id)
    }
    "should move the second to last column to the front" in {
      board = board.moveColumn(2, 0)
      assert(board.columns.length == 4)
      assertResult(board.columns.head.id)(col3.id)
      assertResult(board.columns(1).id)(col0.id)
      assertResult(board.columns(2).id)(col1.id)
      assertResult(board.columns.last.id)(col2.id)
    }
    "should move the first column to the end" in {
      board = board.moveColumn(0, 4)
      assert(board.columns.length == 4)
      assertResult(board.columns.head.id)(col0.id)
      assertResult(board.columns(1).id)(col1.id)
      assertResult(board.columns(2).id)(col2.id)
      assertResult(board.columns.last.id)(col3.id)
    }
    "should remove a column from the board" in {
      val b = board.removeColumn(2) {
        case (id: BoardId, id0: ColumnId) => List.empty[Card]
      }.get

      assert(b.columns.length == 3)
      assert(!b.columns.contains(col2))
      assertResult(b.columns.head.id)(col0.id)
      assertResult(b.columns.tail.head.id)(col1.id)
      assertResult(b.columns.last.id)(col3.id)
    }
  }

  "Cards on a strict Board" should {
    var c: Option[Card] = None

    "be added in the first Column" in {
      c = Card.addToBoard(strictBoard, "card 1", None)
      assert(c.isDefined)
      assertResult(strictBoard.id)(c.get.boardId)
      assertResult(colId0)(c.get.columnId)
    }
    "move to next Column" in {
      assert(c.isDefined)
      c = c.get.move(colId1)(bid => Some(strictBoard))
      assert(c.isDefined)
      assertResult(strictBoard.id)(c.get.boardId)
      assertResult(colId1)(c.get.columnId)

      assert(c.isDefined)
      c = c.get.move(colId2)(bid => Some(strictBoard))
      assert(c.isDefined)

      assert(c.isDefined)
      c = c.get.move(colId3)(bid => Some(strictBoard))
      assert(c.isDefined)
    }
    "fail when moving back past previous Column" in {
      assert(c.get.move(colId0)(bid => Some(strictBoard)).isEmpty)
    }
    "move to previous Column" in {
      assert(c.isDefined)
      c = c.get.move(colId2)(bid => Some(strictBoard))
      assert(c.isDefined)
      assertResult(strictBoard.id)(c.get.boardId)
      assertResult(colId2)(c.get.columnId)
    }
    "fail when moving beyond next Column" in {
      assert(c.isDefined)
      c = c.get.move(colId1)(bid => Some(strictBoard))

      assert(c.get.move(colId3)(bid => Some(strictBoard)).isEmpty)
    }
  }

  "Cards on a non-strict Board" should {
    var c: Option[Card] = None

    "be added in the first Column" in {
      c = Card.addToBoard(nonStrictBoard, "card 1", None)
      assert(c.isDefined)
      assertResult(nonStrictBoard.id)(c.get.boardId)
      assertResult(colId0)(c.get.columnId)
    }
    "move beyond next Column" in {
      assert(c.isDefined)
      c = c.get.move(colId3)(bid => Some(nonStrictBoard))
      assert(c.isDefined)
    }
    "move back past previous Column" in {
      assert(c.isDefined)
      c = c.get.move(colId0)(bid => Some(nonStrictBoard))
      assert(c.isDefined)
    }
    "move to next Column" in {
      assert(c.isDefined)
      c = c.get.move(colId1)(bid => Some(nonStrictBoard))
      assert(c.isDefined)
      assertResult(nonStrictBoard.id)(c.get.boardId)
      assertResult(colId1)(c.get.columnId)
    }
    "move to next Column again" in {
      assert(c.isDefined)
      c = c.get.move(colId2)(bid => Some(nonStrictBoard))
      assert(c.isDefined)
      assert(c.isDefined)
      assertResult(nonStrictBoard.id)(c.get.boardId)
      assertResult(colId2)(c.get.columnId)
    }
    "move to previous Column" in {
      assert(c.isDefined)
      c = c.get.move(colId3)(bid => Some(nonStrictBoard))
      assert(c.isDefined)
      assert(c.isDefined)
      assertResult(nonStrictBoard.id)(c.get.boardId)
      assertResult(colId3)(c.get.columnId)
    }
  }
}

object Boards {

  val bid1 = BoardId(BSONObjectID.generate)
  val bid2 = BoardId(BSONObjectID.generate)

  val nonStrictBoard = Board(
    id = bid1,
    name = "Test Board",
    description = Some("Testing workflow on board"),
    columns = List(col0, col1, col2, col3)
  )

  val strictBoard = nonStrictBoard.copy(
    id = bid2,
    strict = true
  )
}

object Columns {
  val colId0 = ColumnId(BSONObjectID.generate)
  val colId1 = ColumnId(BSONObjectID.generate)
  val colId2 = ColumnId(BSONObjectID.generate)
  val colId3 = ColumnId(BSONObjectID.generate)

  val col0 = Column(colId0, "Backlog", Some("This is a backlog column"))
  val col1 = Column(colId1, "In Progress", Some("Work in progress"))
  val col2 = Column(colId2, "Acceptance", Some("Trolling the internet"))
  val col3 = Column(colId3, "Done", Some("All done amigo"))
}

