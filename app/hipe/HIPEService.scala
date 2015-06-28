/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import hipe.HIPEOperations._
import hipe.core.FailureTypes._
import hipe.core._
import hipe.core.eventstore.HIPECommands.TaskCommand
import hipe.core.eventstore.HIPESupervisor
import hipe.core.eventstore.TaskProtocol.Commands._
import models.base.PersistentType.UserStamp
import models.parties.UserId
import org.slf4j.LoggerFactory

object HIPEService {

  private val logger = LoggerFactory.getLogger(HIPEService.getClass)

  @Singleton
  class ProcessService @Inject()(system: ActorSystem, taskService: TaskService) extends ProcessOperations {

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

    def create(proc: Process): Process = {
      val p = proc.id.map(_ => proc).getOrElse(proc.copy(id = ProcessId.createOpt()))
      Process.save(p)
      findById(p.id.get).getOrElse(p)
    }

    def findById(pid: ProcessId): Option[Process] = Process.findById(pid)

    def update(pid: ProcessId, name: Option[String], strict: Option[Boolean], desc: Option[String]): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(p.copy(
        name = name.fold(p.name)(n => if (n.nonEmpty) n else p.name),
        strict = strict.getOrElse(p.strict),
        description = desc.orElse(p.description))))

    def remove(pid: ProcessId): Unit = Process.delete(pid)

    def addStep(pid: ProcessId, s: Step): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(appendStep(p, s)))

    def addStepAt(pid: ProcessId, s: Step, pos: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(insertStep(p, s, pos)))

    def addGroup(pid: ProcessId, sg: StepGroup): HIPEResult[Process] =
      saveAndReturn(pid)(p => Right(appendGroup(p, sg)))

    def addStepToGroup(pid: ProcessId, sgid: StepGroupId, s: Step): HIPEResult[Process] =
      saveAndReturn(pid)(p => appendStepToGroup(p, sgid, s))

    def addStepToGroupAt(pid: ProcessId, sgid: StepGroupId, s: Step, pos: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => insertStepToGroup(p, sgid, s, pos))

    def removeGroupAt(pid: ProcessId, sgid: StepGroupId): HIPEResult[Process] =
      saveAndReturn(pid)(p =>
        if (!taskService.findByProcessId(pid).exists(t => p.stepGroups.flatten.exists(_.id == t.stepId)))
          removeGroup(p, sgid)
        else
          Left(NotAllowed("It is not allowed to remove a StepGroup with Steps that are referenced by active Tasks."))
      )

    def removeStepAt(pid: ProcessId, sid: StepId) = saveAndReturn(pid) { p =>
      if (!Task.findByProcessId(pid).exists(t => t.stepId == sid)) removeStep(p, sid)
      else Left(NotAllowed("It is not allowed to remove a Step that is referenced by active Tasks."))
    }

    def moveGroupTo(pid: ProcessId, sgid: StepGroupId, to: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => moveStepGroup(p, sgid, to))

    def moveStepTo(cmd: MoveStepCommands.MoveStep): HIPEResult[Process] = {
      import MoveStepCommands._
      cmd match {
        case InGroup(pid, sgid, sid, to) => saveAndReturn(pid)(p => moveStepInGroup(p, sgid, sid, to))
        case ToGroup(pid, sid, dest, pos) => saveAndReturn(pid)(p => moveStepToGroup(p, sid, dest, pos))
        case ToNewGroup(pid, sid, to) => saveAndReturn(pid)(p => moveStepToNewGroup(p, sid, to))
      }
    }

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

    val supervisor = system.actorOf(Props[HIPESupervisor], s"HIPE-supervisor")

    def create(by: UserId, p: Process, t: Task): Option[Task] =
      createTask(by, p, t).map { task =>
        store(task, CreateTask(task.v.get.created.get, task))
        task.id.flatMap(tid => findById(tid)).getOrElse(task)
      }

    def create(by: UserId, p: Process, title: String, desc: Option[String]): Option[Task] =
      createTask(by, p, title, desc).map { task =>
        store(task, CreateTask(task.v.get.created.get, task))
        task.id.flatMap(findById).getOrElse(task)
      }

    def findById(tid: TaskId): Option[Task] = Task.findById(tid)

    def findAllVersions(tid: TaskId): Seq[Task] = Task.findAllVersions(tid)

    def findByProcessId(pid: ProcessId): Seq[Task] = Task.findByProcessId(pid)

    def changeTitle(by: UserId, tid: TaskId, title: String): Option[Task] =
      findById(tid)
        .map(_.copy(title = title))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => ChangeTitle(ust, title)))

    def changeDescription(by: UserId, tid: TaskId, desc: Option[String]): Option[Task] =
      findById(tid)
        .map(_.copy(description = desc))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => ChangeDescription(ust, desc)))

    def complete(by: UserId, tid: TaskId): Option[Task] =
      findById(tid)
        .flatMap(orig => completeAssignment(by, orig))
        .flatMap(t => saveAndReturn(by, t)((ust, t) => CompleteAssignment(ust)))

    def rejectTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => reject(proc, task))((ust, t) => RejectTask(ust))

    def approveTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => approve(proc, task))((ust, t) => ApproveTask(ust))

    def delegateTo(by: UserId, tid: TaskId, userId: UserId): Option[Task] =
      assignOrDelegate(by, tid, userId)((ust, t) => DelegateAssignment(ust, userId))

    def assignTo(by: UserId, tid: TaskId, userId: UserId): Option[Task] =
      assignOrDelegate(by, tid, userId)((ust, t) => ClaimAssignment(ust, t.findAssignmentForUser(userId).get))

    private[this] def assignOrDelegate(by: UserId, tid: TaskId, uid: UserId)(cmd: (UserStamp, Task) => TaskCmd): Option[Task] =
      findById(tid)
        .flatMap(task => assign(task, uid))
        .flatMap(t => saveAndReturn(by, t)(cmd))

    def toStep(by: UserId, tid: TaskId, to: StepId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveTask(p, t, to))((ust, t) => MoveTask(ust, to, t.state))

    def toNextStep(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveToNext(p, t))((ust, t) => MoveTask(ust, t.stepId, t.state))

    def toPreviousStep(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveToPrevious(p, t))((ust, t) => MoveTask(ust, t.stepId, t.state))

    /**
     * find the given process and execute the function f
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
     */
    private[this] def saveAndReturn(by: UserId, t: Task)(cmd: (UserStamp, Task) => TaskCmd): Option[Task] = {
      val ustTuple = t.incrementVersion(by, t.v)
      store(ustTuple._2, cmd(ustTuple._1, ustTuple._2))
      findById(t.id.get)
    }

    private def store(task: Task, cmd: TaskCmd): Unit = {
      val tid = task.id.get
      supervisor ! TaskCommand(tid, cmd)
      Task.save(task)
    }

  }

}
