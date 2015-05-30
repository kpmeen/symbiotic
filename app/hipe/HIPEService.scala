/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.HIPEOperations._
import hipe.core.FailureTypes._
import hipe.core._
import models.parties.UserId
import org.slf4j.LoggerFactory

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

    def moveStepTo(pid: ProcessId, from: Int, to: Int): HIPEResult[Process] =
      saveAndReturn(pid)(p => moveStepGroup(p, from, to))

    def removeStepAt(pid: ProcessId, sid: StepId) = saveAndReturn(pid) { p =>
      if (!Task.findByProcessId(pid).exists(t => t.stepId == sid)) removeStep(p, sid)
      else Left(NotAllowed("It is not allowed to remove a Step that contain active Tasks."))

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

    def create(p: Process, t: Task): Option[Task] =
      createTask(p, t).map { task =>
        Task.save(task)
        task.id.flatMap(findById).getOrElse(task)
      }

    def findById(tid: TaskId): Option[Task] = Task.findById(tid)

    def findByProcessId(pid: ProcessId): Seq[Task] = Task.findByProcessId(pid)

    def update(tid: TaskId, t: Task): Option[Task] = {
      // TODO....first do diff...then save and return data
      Task.save(t)
      findById(tid)
    }

    def complete(tid: TaskId, userId: UserId): Option[Task] =
      findById(tid).map(task => completeAssignment(task, userId)).map(saveAndReturn)

    def rejectTask(tid: TaskId): Option[Task] = saveTask(tid)((proc, task) => reject(proc, task))

    def approveTask(tid: TaskId): Option[Task] = saveTask(tid)((proc, task) => approve(proc, task))

    def delegateTo(tid: TaskId, userId: UserId): Option[Task] = assignTo(tid, userId)

    def assignTo(tid: TaskId, userId: UserId): Option[Task] =
      findById(tid).map(task => assign(task, userId)).map(saveAndReturn)

    def toStep(tid: TaskId, to: StepId): HIPEResult[Task] =
      saveTask(tid)((p, t) => moveTask(p, t, to))

    def toNextStep(tid: TaskId): HIPEResult[Task] =
      saveTask(tid)((p, t) => moveToNext(p, t))

    def toPreviousStep(tid: TaskId): HIPEResult[Task] =
      saveTask(tid)((p, t) => moveToPrevious(p, t))

    /**
     * find the given process and execute the function f
     */
    private[this] def saveTask(tid: TaskId)(f: (Process, Task) => HIPEResult[Task]): HIPEResult[Task] =
      findById(tid).map { t =>
        Process.findById(t.processId).map { p =>
          f(p, t) match {
            case Right(task) =>
              Task.save(task)
              findById(tid).toRight(VeryBad(s"Could not find task $tid after saving"))
            case Left(err) => Left(err)
          }
        }.getOrElse(Left(NotFound(s"Could not find process ${t.processId}")))
      }.getOrElse(Left(NotFound(s"Could not find task $tid")))

    /**
     * Silly function to avoid having to do the same crap over and over again...
     */
    private[this] def saveAndReturn(t: Task): Task = {
      Task.save(t)
      t
    }

  }

}
