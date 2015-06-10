/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core.eventstore

import com.mongodb.casbah.Imports._
import hipe.core.States.TaskState
import hipe.core.{Assignment, StepId, Task}
import models.base.PersistentType.UserStamp
import models.parties.UserId

object TaskProtocol {

  object Commands {

    sealed trait TaskCmd

    case class CreateTask(by: UserStamp, t: Task) extends TaskCmd

    case class ChangeTitle(by: UserStamp, title: String) extends TaskCmd

    case class ChangeDescription(by: UserStamp, desc: Option[String]) extends TaskCmd

    case class ClaimAssignment(by: UserStamp, assignment: Assignment) extends TaskCmd

    case class CompleteAssignment(by: UserStamp) extends TaskCmd

    case class DelegateAssignment(by: UserStamp, to: UserId) extends TaskCmd

    case class AddAssignment(by: UserStamp, assignment: Assignment) extends TaskCmd

    case class MoveTask(by: UserStamp, stepId: StepId, state: TaskState) extends TaskCmd

    case class ApproveTask(by: UserStamp) extends TaskCmd

    case class RejectTask(by: UserStamp) extends TaskCmd

    case class ConsolidateTask(by: UserStamp) extends TaskCmd

  }

  object Events {

    private object EventTypeStrings {
      val taskCreated = "TaskCreated"
      val titleChanged = "TitleChanged"
      val descriptionChanged = "DescriptionChanged"
      val assignmentClaimed = "AssignmentClaimed"
      val assignmentCompleted = "AssignmentCompleted"
      val assignmentDelegated = "AssignmentDelegated"
      val assignmentAdded = "AssignmentAdded"
      val taskMoved = "TaskMoved"
      val taskApproved = "TaskApproved"
      val taskRejected = "TaskRejected"
      val taskConsolidated = "TaskConsolidated"
    }

    sealed trait TaskEvent {
      val eventType: String
    }

    case class TaskCreated(by: UserStamp, t: Task) extends TaskEvent {
      override val eventType = EventTypeStrings.taskCreated
    }

    object TaskCreated {
      implicit def toBSON(evt: TaskCreated): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "task" -> Task.toBSON(evt.t)
        )

      implicit def fromBSON(dbo: DBObject): TaskCreated =
        TaskCreated(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          t = Task.fromBSON(dbo.as[DBObject]("task"))
        )
    }

    case class TitleChanged(by: UserStamp, title: String) extends TaskEvent {
      override val eventType = EventTypeStrings.titleChanged
    }

    object TitleChanged {
      implicit def toBSON(evt: TitleChanged): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "title" -> evt.title
        )

      implicit def fromBSON(dbo: DBObject): TitleChanged =
        TitleChanged(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          title = dbo.as[String]("title")
        )
    }

    case class DescriptionChanged(by: UserStamp, desc: Option[String]) extends TaskEvent {
      override val eventType = EventTypeStrings.descriptionChanged
    }

    object DescriptionChanged {
      implicit def toBSON(evt: DescriptionChanged): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "desc" -> evt.desc.getOrElse("")
        )

      implicit def fromBSON(dbo: DBObject): DescriptionChanged =
        DescriptionChanged(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          desc = dbo.getAs[String]("desc")
        )
    }

    case class AssignmentAdded(by: UserStamp, assignment: Assignment) extends TaskEvent {
      override val eventType = EventTypeStrings.assignmentAdded
    }

    object AssignmentAdded {
      implicit def toBSON(evt: AssignmentAdded): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "assignment" -> Assignment.toBSON(evt.assignment)
        )

      implicit def fromBSON(dbo: DBObject): AssignmentAdded =
        AssignmentAdded(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          assignment = Assignment.fromBSON(dbo.as[DBObject]("assignment"))
        )
    }

    case class AssignmentClaimed(by: UserStamp, assignment: Assignment) extends TaskEvent {
      override val eventType = EventTypeStrings.assignmentClaimed
    }

    object AssignmentClaimed {
      implicit def toBSON(evt: AssignmentClaimed): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "assignment" -> Assignment.toBSON(evt.assignment)
        )

      implicit def fromBSON(dbo: DBObject): AssignmentClaimed =
        AssignmentClaimed(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          assignment = Assignment.fromBSON(dbo.as[DBObject]("assignment"))
        )
    }

    case class AssignmentCompleted(by: UserStamp) extends TaskEvent {
      override val eventType = EventTypeStrings.assignmentCompleted
    }

    object AssignmentCompleted {
      implicit def toBSON(evt: AssignmentCompleted): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by)
        )

      implicit def fromBSON(dbo: DBObject): AssignmentCompleted =
        AssignmentCompleted(UserStamp.fromBSON(dbo.as[DBObject]("by")))
    }

    case class AssignmentDelegated(by: UserStamp, to: UserId) extends TaskEvent {
      override val eventType = EventTypeStrings.assignmentDelegated
    }

    object AssignmentDelegated {
      implicit def toBSON(evt: AssignmentDelegated): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "to" -> evt.to.value
        )

      implicit def fromBSON(dbo: DBObject): AssignmentDelegated =
        AssignmentDelegated(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          to = dbo.as[String]("to")
        )
    }

    case class TaskMoved(by: UserStamp, stepId: StepId, state: TaskState) extends TaskEvent {
      override val eventType = EventTypeStrings.taskMoved
    }

    object TaskMoved {
      implicit def toBSON(evt: TaskMoved): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by),
          "stepId" -> evt.stepId.value,
          "state" -> TaskState.asString(evt.state)
        )

      implicit def fromBSON(dbo: DBObject): TaskMoved =
        TaskMoved(
          by = UserStamp.fromBSON(dbo.as[DBObject]("by")),
          stepId = StepId.asId(dbo.as[String]("stepId")),
          state = TaskState.asState(dbo.as[String]("state"))
        )
    }

    case class TaskApproved(by: UserStamp) extends TaskEvent {
      override val eventType = EventTypeStrings.taskApproved
    }

    object TaskApproved {
      implicit def toBSON(evt: TaskApproved): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by)
        )

      implicit def fromBSON(dbo: DBObject): TaskApproved =
        TaskApproved(UserStamp.fromBSON(dbo.as[DBObject]("by")))
    }

    case class TaskRejected(by: UserStamp) extends TaskEvent {
      override val eventType = EventTypeStrings.taskRejected
    }

    object TaskRejected {
      implicit def toBSON(evt: TaskRejected): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by)
        )

      implicit def fromBSON(dbo: DBObject): TaskRejected =
        TaskRejected(UserStamp.fromBSON(dbo.as[DBObject]("by")))
    }

    case class TaskConsolidated(by: UserStamp) extends TaskEvent {
      override val eventType = EventTypeStrings.taskConsolidated
    }

    object TaskConsolidated {
      implicit def toBSON(evt: TaskConsolidated): DBObject =
        MongoDBObject(
          "eventType" -> evt.eventType,
          "by" -> UserStamp.toBSON(evt.by)
        )

      implicit def fromBSON(dbo: DBObject): TaskConsolidated =
        TaskConsolidated(UserStamp.fromBSON(dbo.as[DBObject]("by")))
    }

    object TaskEvent {
      implicit def fromBSON(dbo: DBObject): TaskEvent = {
        import EventTypeStrings._
        val eventType = dbo.as[String]("eventType")

        eventType match {
          case `taskCreated` => TaskCreated.fromBSON(dbo)
          case `titleChanged` => TitleChanged.fromBSON(dbo)
          case `descriptionChanged` => DescriptionChanged.fromBSON(dbo)
          case `assignmentClaimed` => AssignmentClaimed.fromBSON(dbo)
          case `assignmentCompleted` => AssignmentCompleted.fromBSON(dbo)
          case `assignmentDelegated` => AssignmentDelegated.fromBSON(dbo)
          case `assignmentAdded` => AssignmentAdded.fromBSON(dbo)
          case `taskMoved` => TaskMoved.fromBSON(dbo)
          case `taskApproved` => TaskApproved.fromBSON(dbo)
          case `taskRejected` => TaskRejected.fromBSON(dbo)
          case `taskConsolidated` => TaskConsolidated.fromBSON(dbo)
          case unsupported => throw new IllegalArgumentException(s"Could not instantiate TaskEvent $unsupported")
        }
      }

      implicit def toBSON(e: TaskEvent): DBObject = {
        e match {
          case evt: TaskCreated => TaskCreated.toBSON(evt)
          case evt: TitleChanged => TitleChanged.toBSON(evt)
          case evt: DescriptionChanged => DescriptionChanged.toBSON(evt)
          case evt: AssignmentClaimed => AssignmentClaimed.toBSON(evt)
          case evt: AssignmentCompleted => AssignmentCompleted.toBSON(evt)
          case evt: AssignmentDelegated => AssignmentDelegated.toBSON(evt)
          case evt: AssignmentAdded => AssignmentAdded.toBSON(evt)
          case evt: TaskMoved => TaskMoved.toBSON(evt)
          case evt: TaskApproved => TaskApproved.toBSON(evt)
          case evt: TaskRejected => TaskRejected.toBSON(evt)
          case evt: TaskConsolidated => TaskConsolidated.toBSON(evt)
          case unsupported => throw new IllegalArgumentException(s"Can not serialise TaskEvent to BSON $unsupported")
        }
      }
    }

  }

}