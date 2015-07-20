/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import hipe.core.FailureTypes._
import hipe.core._
import hipe.core.eventstore.HIPECommands.TaskCommand
import hipe.core.eventstore.HIPESupervisor
import hipe.core.eventstore.TaskProtocol.Commands._
import models.base.PersistentType.UserStamp
import models.parties.UserId
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

object HIPEService {

  private val logger = LoggerFactory.getLogger(HIPEService.getClass)

  // TODO: This should not be done here..
  Process.ensureIndex()
  Task.ensureIndex()

  @Singleton
  class ProcessService @Inject()(system: ActorSystem, taskService: TaskService) extends ProcessOperations {

    /**
     * Create a new Process with the given params
     *
     * @param name the name of the process
     * @param strict true if the process should have strict rules, else false
     * @param desc a description of the process
     * @return The newly created Process
     */
    def create(name: String, strict: Boolean, desc: Option[String]): Process = {
      val pid = ProcessId.create()
      val p = Process(
        id = Some(pid),
        name = name,
        strict = strict,
        description = desc)
      Process.save(p)
      findById(pid).getOrElse(p)
    }

    /**
     * Create the Process passed in the proc argument
     *
     * @param proc the Process to create
     * @return The newly created Process
     */
    def create(proc: Process): Process = {
      val p = proc.id.map(_ => proc).getOrElse(proc.copy(id = ProcessId.createOpt()))
      Process.save(p)
      findById(p.id.get).getOrElse(p)
    }

    /**
     * Try to locate the Process with the given ProcessId
     *
     * @param pid the ProcessId to look for
     * @return an Option of Process
     */
    def findById(pid: ProcessId): Option[Process] = Process.findById(pid)

    /**
     * Update the process with the provided arguments.
     *
     * @param pid the ProcessId to update
     * @param name an Optional process name
     * @param strict an Optional flag indicating strict or lenient process
     * @param desc an Optional process description
     * @return A HIPEResult of Process
     */
    def update(pid: ProcessId, name: Option[String], strict: Option[Boolean], desc: Option[String]): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(p.copy(
        name = name.fold(p.name)(n => if (n.nonEmpty) n else p.name),
        strict = strict.getOrElse(p.strict),
        description = desc.orElse(p.description))))

    /**
     * Removes the Process with the given ProcessId
     *
     * @param pid the ProcessId to remove
     */
    def remove(pid: ProcessId): Unit = Process.delete(pid)

    /**
     * Appends a Step to the end of the Process
     *
     * @param pid the ProcessId of the Process
     * @param s the Step to append
     * @return A HIPEResult of Process
     */
    def addStep(pid: ProcessId, s: Step): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(appendStep(p, s)))

    /**
     * Inserts a new Step to the given position in the Process
     *
     * @param pid the ProcessId of the Process
     * @param s the Step to add
     * @param pos the position in the Process to add the Step
     * @return A HIPEResult of Process
     */
    def addStepAt(pid: ProcessId, s: Step, pos: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(insertStep(p, s, pos)))

    /**
     * Appends a new StepGroup to the Process
     *
     * @param pid the ProcessId of the Process
     * @param sg the StepGroup to append
     * @return A HIPEResult of Process
     */
    def addGroup(pid: ProcessId, sg: StepGroup): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(appendGroup(p, sg)))

    /**
     * Appends a new Step to the StepGroup with the given StepGroupId
     *
     * @param pid the ProcessId of the Process
     * @param sgid the StepGroupId to append a Step to
     * @param s the Step to append
     * @return A HIPEResult of Process
     */
    def addStepToGroup(pid: ProcessId, sgid: StepGroupId, s: Step): HIPEResult[Process] =
      saveAndReturn(pid)(p => appendStepToGroup(p, sgid, s))

    /**
     * Adds a Step at the given position in the StepGroup with provided StepGroupId
     *
     * @param pid the ProcessId of the Process
     * @param sgid the StepGroupId to add a Step to
     * @param s the Step to add
     * @param pos the position in the StepGroup to add the Step at
     * @return A HIPEResult of Process
     */
    def addStepToGroupAt(pid: ProcessId, sgid: StepGroupId, s: Step, pos: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => insertStepToGroup(p, sgid, s, pos))

    /**
     * Removes the entire StepGroup with the given StepGroupId
     *
     * @param pid the ProcessId of the Process
     * @param sgid the StepGroupId to remove
     * @return A HIPEResult of Process
     */
    def removeGroupAt(pid: ProcessId, sgid: StepGroupId): HIPEResult[Process] =
      saveAndReturn(pid)(p =>
        if (!taskService.findByProcessId(pid).exists(t => p.stepGroups.flatten.exists(_.id == t.stepId)))
          removeGroup(p, sgid)
        else
          Left(NotAllowed("It is not allowed to remove a StepGroup with Steps that are referenced by active Tasks."))
      )

    /**
     * Removes the Step with the given StepId
     *
     * @param pid the ProcessId of the Process
     * @param sid the StepId to remove
     * @return A HIPEResult of Process
     */
    def removeStepAt(pid: ProcessId, sid: StepId): HIPEResult[Process] = saveAndReturn(pid) { p =>
      if (!Task.findByProcessId(pid).exists(t => t.stepId == sid)) removeStep(p, sid)
      else Left(NotAllowed("It is not allowed to remove a Step that is referenced by active Tasks."))
    }

    /**
     * Moves the StepGroup with the given StepGroupId to the specified postion in the Process
     *
     * @param pid the ProcessId of the Process
     * @param sgid the StepGroupId to move
     * @param to the position to move the StepGroup to
     * @return A HIPEResult of Process
     */
    def moveGroupTo(pid: ProcessId, sgid: StepGroupId, to: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => moveStepGroup(p, sgid, to))

    /**
     * This method enables moving a Step to a new position in the Process. The argument {{{cmd: MoveStep}}} is
     * an ADT that consists of 3 types of commands:
     * <ul>
     * <li>InGroup: moving a Step to a new position within a specific StepGroup</li>
     * <li>ToGroup: moving a Step to a given position in another StepGroup</li>
     * <li>ToNewGroup: moving a Step to a brand new StepGroup</li>
     * </ul>
     *
     * @param cmd MoveStep command
     * @return A HIPEResult of Process
     * @see hipe.MoveStepCommands.MoveStep
     */
    def moveStepTo(cmd: MoveStepCommands.MoveStep): HIPEResult[Process] = {
      import MoveStepCommands._
      cmd match {
        case InGroup(pid, sgid, sid, to) => saveAndReturn(pid)(p => moveStepInGroup(p, sgid, sid, to))
        case ToGroup(pid, sid, dest, pos) => saveAndReturn(pid)(p => moveStepToGroup(p, sid, dest, pos))
        case ToNewGroup(pid, sid, to) => saveAndReturn(pid)(p => moveStepToNewGroup(p, sid, to))
      }
    }

    /**
     * Private helper method for persisting the changes made to a Process.
     *
     * @param pid the ProcessId of the Process
     * @param f function to do some operation on the provided Process data before saving
     * @return A HIPEResult of Process
     */
    private[this] def saveAndReturn(pid: ProcessId)(f: Process => HIPEResult[Process]): HIPEResult[Process] =
      Process.findById(pid).map { p =>
        f(p) match {
          case Right(proc) =>
            logger.trace(s"Saving $proc")
            Process.save(proc)
            findById(pid).map(Right(_)).getOrElse(Left(VeryBad("Could not find process after update")))
          case Left(err) => Left(err)
        }
      }.getOrElse(Left(NotFound(s"Operation on process $pid failed with value None")))
  }

  @Singleton
  class TaskService @Inject()(system: ActorSystem) extends TaskOperations {

    import Implicits._

    // This is the supervising Actor that takes care of handling event-sourcing functionality
    // on the different Tasks. There will only be 1 of these in a running application.
    val supervisor = system.actorOf(Props[HIPESupervisor], s"HIPE-supervisor")

    /**
     * Create a new Task.
     *
     * @param by UserId of the user performing the action
     * @param p the Process for which to create a Task
     * @param t the Task to create
     * @return an Option of Task. Will be None if the Task wasn't created.
     */
    def create(by: UserId, p: Process, t: Task): Option[Task] =
      createTask(by, p, t).map { task =>
        store(task, CreateTask(task.v.get.created.get, task))
        task.id.flatMap(tid => findById(tid)).getOrElse(task)
      }

    /**
     * Create a new Task.
     *
     * @param by UserId of the user performing the action
     * @param p the Process for which to create a Task
     * @param title the title text of the Task
     * @param desc an Optional description
     * @param due an Optional DateTime indicating due-date
     * @return an Option of Task. Will be None if the Task wasn't created.
     */
    def create(by: UserId, p: Process, title: String, desc: Option[String], due: Option[DateTime]): Option[Task] =
      createTask(by, p, title, desc, due).map { task =>
        store(task, CreateTask(task.v.get.created.get, task))
        task.id.flatMap(findById).getOrElse(task)
      }

    /**
     * Find the task with the given TaskId.
     *
     * @param tid the TaskId to find.
     * @return an Option of Task.
     */
    def findById(tid: TaskId): Option[Task] = Task.findById(tid)

    /**
     * Find all versions of the task with the given TaskId. If no matching Tasks were found,
     * the returned collection will be empty.
     *
     * @param tid the TaskId to find.
     * @return A collection of Tasks
     */
    def findAllVersions(tid: TaskId): Seq[Task] = Task.findAllVersions(tid)

    /**
     * Find all Tasks for the given ProcessId. If no matching Tasks were found, the
     * returned collection will be empty.
     *
     * @param pid
     * @return
     */
    def findByProcessId(pid: ProcessId): Seq[Task] = Task.findByProcessId(pid)

    /**
     * Method for changing the title of a Task.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to modify
     * @param title the new Title to set
     * @return an Option of Task.
     */
    def changeTitle(by: UserId, tid: TaskId, title: String): Option[Task] =
      findById(tid)
        .map(_.copy(title = title))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => ChangeTitle(ust, title)))

    /**
     * Method for changing the description of a Task.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to modify
     * @param desc an Optional descriptive text. (To unset description, pass in None)
     * @return an Option of Task.
     */
    def changeDescription(by: UserId, tid: TaskId, desc: Option[String]): Option[Task] =
      findById(tid)
        .map(_.copy(description = desc))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => ChangeDescription(ust, desc)))

    /**
     * Method for changing the due date of a Task.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to modify
     * @param due an Optional DateTime representing the due-date for the Task
     * @return an Option of Task.
     */
    def changeDueDate(by: UserId, tid: TaskId, due: Option[DateTime]): Option[Task] =
      findById(tid)
        .map(_.copy(dueDate = due))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => ChangeDueDate(ust, due)))

    /**
     * Complete and assignment on the specified Task where the given UserId is the assignee.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to complete an assignment for
     * @return an Option of Task.
     */
    def complete(by: UserId, tid: TaskId): Option[Task] =
      findById(tid)
        .flatMap(orig => completeAssignment(by, orig))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => CompleteAssignment(ust)))

    /**
     * Reject a Task.
     *
     * This is similar to {{{declineTask}}}. So why are there two methods doing the same thing?
     * The short answer is; Sometimes it is desirable to have different behaviour depending on the action.
     * And, typically, a rejection is a "stronger" action than declining. The idea is that the "reject" action
     * is for scenarios where the Task should be moved to an earlier Step for another iteration. While
     * "decline" can be used for situations where the process should proceed to a Step further in the Process,
     * as a negative (but valid) answer.
     *
     * By default a reject will move to the previous step (regardless of StepGroup).
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to reject
     * @return an Option of Task.
     */
    def rejectTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => reject(proc, task))((ust, t) => RejectTask(ust, t.dueDate))

    /**
     * Decline a Task. See {{{rejectTask}}}.
     *
     * By default a decline will move to the previous step (regardless of StepGroup).
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to decline
     * @return an Option of Task.
     */
    def declineTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => decline(proc, task))((ust, t) => DeclineTask(ust, t.dueDate))

    /**
     * Approve a Task. Will automatically move the Task to the next Step in its transition configuration.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to approve
     * @return an Option of Task.
     */
    def approveTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => approve(proc, task))((ust, t) => ApproveTask(ust, t.dueDate))

    /**
     * Consolidate all assignments in a Task. Will automatically move the Task to the next Step in its
     * transition configuration.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to consolidate
     * @return an Option of Task.
     */
    def consolidateTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => consolidate(proc, task))((ust, t) => ConsolidateTask(ust, t.dueDate))

    /**
     * Delegate an Assignment on a Task to the given UserId.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to delegate an assignment on
     * @param uid the UserId of the User to assign the task
     * @return an Option of Task.
     */
    def delegateTo(by: UserId, tid: TaskId, uid: UserId): Option[Task] =
      assignOrDelegate(by, tid, uid)((ust, t) => DelegateAssignment(ust, uid))

    /**
     * Assign an Assignment on a Task to the given UserId.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task assign an assignment on
     * @param uid the UserId of the User to assign the task
     * @return an Option of Task.
     */
    def assignTo(by: UserId, tid: TaskId, uid: UserId): Option[Task] =
      assignOrDelegate(by, tid, uid)((ust, t) => ClaimAssignment(ust, t.findAssignmentForUser(uid).get))

    /**
     * Assign and delegate are in effect the same operation. They just happen to be to distinct actions
     * that can be performed. Because they have different intents, they should be semantically separated.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task
     * @param uid the UserId of the User to assign the task
     * @param cmd function that should return the appropriate TaskCmd (ClaimAssignment or DelegateAssignment)
     * @return an Option of Task.
     */
    private[this] def assignOrDelegate(by: UserId, tid: TaskId, uid: UserId)(cmd: (UserStamp, Task) => TaskCmd): Option[Task] =
      findById(tid)
        .flatMap(task => assign(task, uid))
        .flatMap(t => saveAndReturn(by, t)(cmd))

    /**
     * Will add a new Assignment to the specified Task.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task add an assignment on
     * @return a HIPEResult of Task.
     */
    def addAssignment(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((proc, task) => newAssignment(proc, task))((ust, t) => AddAssignment(ust, t.assignments.last))

    /**
     * Move a Task to the Step with the given StepId.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to move
     * @param to the StepId of the Step to move to
     * @return a HIPEResult of Task.
     */
    def toStep(by: UserId, tid: TaskId, to: StepId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveTask(p, t, to))((ust, t) => MoveTask(ust, to, t.state, t.dueDate))

    /**
     * Move a Task to the next Step in the process.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to move
     * @return a HIPEResult of Task.
     */
    def toNextStep(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveToNext(p, t))((ust, t) => MoveTask(ust, t.stepId, t.state, t.dueDate))

    /**
     * Move a Task to the previous Step in the process.
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task to move
     * @return a HIPEResult of Task.
     */
    def toPreviousStep(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveToPrevious(p, t))((ust, t) => MoveTask(ust, t.stepId, t.state, t.dueDate))

    /**
     * Find the given process and execute the function f
     *
     * @param by UserId of the user performing the action
     * @param tid the TaskId of the Task
     * @param f function that should do some processing on (Process, Task) and return a HIPEResult of Task.
     * @param cmd function that should return the appropriate TaskCmd
     * @return a HIPEResult of Task.
     */
    private[this] def saveTask(by: UserId, tid: TaskId)(f: (Process, Task) => HIPEResult[Task])(cmd: (UserStamp, Task) => TaskCmd): HIPEResult[Task] =
      findById(tid).map { t =>
        Process.findById(t.processId).map { p =>
          f(p, t) match {
            case Right(task) =>
              saveAndReturn(by, task)(cmd).toRight(VeryBad(s"Could not find task $tid after saving"))
            case Left(err) => Left(err)
          }
        }.getOrElse(Left(NotFound(s"Could not find process ${t.processId}")))
      }.getOrElse(Left(NotFound(s"Could not find task $tid")))

    /**
     * Silly function to avoid having to do the same crap over and over again...
     *
     * @param by UserId of the user performing the action
     * @param t the Task to save
     * @param cmd function that should return the appropriate TaskCmd
     * @return an Option of Task.
     */
    private[this] def saveAndReturn(by: UserId, t: Task)(cmd: (UserStamp, Task) => TaskCmd): Option[Task] = {
      val ustTuple = t.incrementVersion(by, t.v)
      store(ustTuple._2, cmd(ustTuple._1, ustTuple._2))
      findById(t.id.get)
    }

    /**
     * Sends the TaskCommand to the event-sourced journal and saves the Task.
     *
     * @param task the Task to save
     * @param cmd the TaskCmd to register in the Journal
     */
    private def store(task: Task, cmd: TaskCmd): Unit = {
      val tid = task.id.get
      supervisor ! TaskCommand(tid, cmd)
      Task.save(task)
    }

  }

}
