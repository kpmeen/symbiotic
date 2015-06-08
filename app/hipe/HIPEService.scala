/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations._
import hipe.core.FailureTypes._
import hipe.core._
import models.base.PersistentType.UserStamp
import models.parties.UserId
import org.slf4j.LoggerFactory
import play.api.libs.json._

object HIPEService {

  private val logger = LoggerFactory.getLogger(HIPEService.getClass)

  object ProcessService extends ProcessOperations {

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

    def addStepToGroup(pid: ProcessId, sgid: StepGroupId, s: Step): HIPEResult[Process] =
      saveAndReturn(pid)(p => appendStepToGroup(p, sgid, s))

    def addStepToGroupAt(pid: ProcessId, sgid: StepGroupId, s: Step, pos: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => insertStepToGroup(p, sgid, s, pos))

    def removeGroupAt(pid: ProcessId, sgid: StepGroupId): HIPEResult[Process] =
      saveAndReturn(pid)(p =>
        if (!TaskService.findByProcessId(pid).exists(t => p.stepGroups.flatten.exists(_.id == t.stepId)))
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

    /**
     * Contains command definitions for the below move functions
     *
     * TODO: Refactor to package object
     */
    object MoveStepCommands {

      private[this] val CommandAttr = "command"
      private[this] val InGroupCmd = "InGroup"
      private[this] val ToGroupCmd = "ToGroup"
      private[this] val ToNewGroupCmd = "ToNewGroup"

      sealed trait MoveStep {
        val commandName: String
      }

      case class InGroup(pid: ProcessId, sgid: StepGroupId, sid: StepId, to: Int) extends MoveStep {
        override val commandName: String = InGroupCmd
      }

      case class ToGroup(pid: ProcessId, sid: StepId, dest: StepGroupId, pos: Int) extends MoveStep {
        override val commandName: String = ToGroupCmd
      }

      case class ToNewGroup(pid: ProcessId, sid: StepId, to: Int) extends MoveStep {
        override val commandName: String = ToNewGroupCmd
      }

      private[this] val ingrpFormat: Format[InGroup] = Json.format[InGroup]
      private[this] val togrpFormat: Format[ToGroup] = Json.format[ToGroup]
      private[this] val tngrpFormat: Format[ToNewGroup] = Json.format[ToNewGroup]

      implicit val reads: Reads[MoveStep] = Reads { jsv =>
        (jsv \ CommandAttr).as[String] match {
          case `InGroupCmd` => JsSuccess(jsv.as(ingrpFormat))
          case `ToGroupCmd` => JsSuccess(jsv.as(togrpFormat))
          case `ToNewGroupCmd` => JsSuccess(jsv.as(tngrpFormat))
          case err => JsError(s"Not a supported MoveStep command: $err")
        }
      }

      implicit val writes: Writes[MoveStep] = Writes {
        case ing: InGroup => ingrpFormat.writes(ing).as[JsObject] ++ Json.obj(CommandAttr -> InGroupCmd)
        case tog: ToGroup => togrpFormat.writes(tog).as[JsObject] ++ Json.obj(CommandAttr -> ToGroupCmd)
        case tng: ToNewGroup => tngrpFormat.writes(tng).as[JsObject] ++ Json.obj(CommandAttr -> ToNewGroupCmd)
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

  object TaskService extends TaskOperations {

    def create(by: UserId, p: Process, t: Task): Option[Task] =
      createTask(by, p, t).map { task =>
        Task.save(task)
        task.id.flatMap(tid => findById(tid)).getOrElse(task)
      }

    def create(by: UserId, p: Process, title: String, desc: Option[String]): Option[Task] =
      createTask(by, p, title, desc).map { task =>
        Task.save(task)
        task.id.flatMap(findById).getOrElse(task)
      }

    def findById(tid: TaskId): Option[Task] = Task.findById(tid)

    def findByProcessId(pid: ProcessId): Seq[Task] = Task.findByProcessId(pid)

    def update(by: UserId, tid: TaskId, t: Task): Option[Task] = {
      // FIXME: Need to take proper care of versioning here...
      findById(tid).flatMap { orig =>
        Task.save(t.incrementVersion(by, orig.v))
        findById(tid)
      }
    }

    def complete(by: UserId, tid: TaskId): Option[Task] =
      findById(tid).flatMap { orig =>
        completeAssignment(by, orig)
      }.map(t => saveAndReturn(by, t))

    def rejectTask(by: UserId, tid: TaskId): Option[Task] =
      saveTask(by, tid)((proc, task) => reject(proc, task))

    def approveTask(by: UserId, tid: TaskId): Option[Task] = saveTask(by, tid)((proc, task) => approve(proc, task))

    def delegateTo(by: UserId, tid: TaskId, userId: UserId): Option[Task] = assignTo(by, tid, userId)

    def assignTo(by: UserId, tid: TaskId, userId: UserId): Option[Task] =
      findById(tid).flatMap(task => assign(task, userId)).map(t => saveAndReturn(by, t))

    def toStep(by: UserId, tid: TaskId, to: StepId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveTask(p, t, to))

    def toNextStep(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveToNext(p, t))

    def toPreviousStep(by: UserId, tid: TaskId): HIPEResult[Task] =
      saveTask(by, tid)((p, t) => moveToPrevious(p, t))

    /**
     * find the given process and execute the function f
     */
    private[this] def saveTask(by: UserId, tid: TaskId)(f: (Process, Task) => HIPEResult[Task]): HIPEResult[Task] =
      findById(tid).map { t =>
        Process.findById(t.processId).map { p =>
          f(p, t) match {
            case Right(task) =>
              Task.save(task.incrementVersion(by, t.v))
              findById(tid).toRight(VeryBad(s"Could not find task $tid after saving"))
            case Left(err) => Left(err)
          }
        }.getOrElse(Left(NotFound(s"Could not find process ${t.processId}")))
      }.getOrElse(Left(NotFound(s"Could not find task $tid")))

    /**
     * Silly function to avoid having to do the same crap over and over again...
     */
    private[this] def saveAndReturn(by: UserId, t: Task): Task = {
      val tsk = t.incrementVersion(by, t.v)
      Task.save(tsk)
      tsk
    }

  }

}
