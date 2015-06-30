/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core.eventstore

import akka.actor.ActorLogging
import akka.persistence._
import hipe.TaskOperations
import hipe.core.TaskId
import hipe.core.eventstore.HIPECommands.{Print, Snapshot}
import hipe.core.eventstore.TaskProtocol.Commands._
import hipe.core.eventstore.TaskProtocol.Events._

case class State(events: List[TaskEvent] = Nil) {
  def updated(evt: TaskEvent): State = copy(evt :: events)

  def size: Int = events.length

  override def toString: String = events.reverse.mkString("\n")
}

class TaskProcessor(tid: TaskId) extends PersistentActor with TaskOperations with ActorLogging {
  var state = State()

  def updateState(event: TaskEvent): Unit = {
    log.debug(s"Updating state with event:\n$event")
    state = state.updated(event)
  }

  def numEvents = state.size

  override def persistenceId: String = s"p-${tid.value}"

  override def receiveRecover: Receive = {
    case evt: TaskEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case CreateTask(by, task) => journalAndPublish(TaskCreated(by, task))
    case ChangeTitle(by, title) => journalAndPublish(TitleChanged(by, title))
    case ChangeDescription(by, desc) => journalAndPublish(DescriptionChanged(by, desc))
    case ClaimAssignment(by, assignment) => journalAndPublish(AssignmentClaimed(by, assignment))
    case CompleteAssignment(by) => journalAndPublish(AssignmentCompleted(by))
    case DelegateAssignment(by, to) => journalAndPublish(AssignmentDelegated(by, to))
    case AddAssignment(by, assignment) => journalAndPublish(AssignmentAdded(by, assignment))
    case MoveTask(by, stepId, tstate) => journalAndPublish(TaskMoved(by, stepId, tstate))
    case ApproveTask(by) => journalAndPublish(TaskApproved(by))
    case RejectTask(by) => journalAndPublish(TaskRejected(by))
    case DeclineTask(by) => journalAndPublish(TaskDeclined(by))
    case ConsolidateTask(by) => journalAndPublish(TaskConsolidated(by))

    case s: Snapshot => saveSnapshot(state)
    case p: Print => println(state.events.mkString("\n"))
    case evt => log.warning(s"Event type not supported: $evt")
  }

  def journalAndPublish[A <: TaskEvent](evt: A): Unit =
    persist(TaskEvent.toBSON(evt)) { dbo =>
      updateState(evt)
      context.system.eventStream.publish(evt)
    }
}