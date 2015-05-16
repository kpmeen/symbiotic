/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core._
import models.parties.UserId
import org.bson.types.ObjectId

object HIPEService extends ProcessOperations with TaskOperations {

  private[this] def saveAndReturn[A](maybe: Option[A])(f: A => Option[A]): Option[A] =
    maybe.map { m =>
      f(m)
      m
    }

  object ProcessService {

    def create(name: String, strict: Boolean, desc: Option[String]): Process = {
      val p = Process(
        id = Some(ProcessId(new ObjectId().toString)),
        name = name,
        strict = strict,
        description = desc
      )
      Process.save(p)
      p
    }

    def findById(pid: ProcessId): Option[Process] = Process.findById(pid)

    def update(pid: ProcessId, name: Option[String], strict: Option[Boolean], desc: Option[String]): Option[Process] =
      saveAndReturnProcess(pid)(p => Option(p.copy(
        name = name.fold(p.name)(n => if (n.nonEmpty) n else p.name),
        strict = strict.getOrElse(p.strict),
        description = desc.orElse(p.description)
      )))

    def remove(pid: ProcessId): Unit = Process.delete(pid)

    def addStep(pid: ProcessId, s: Step): Option[Process] = saveAndReturnProcess(pid)(p => Option(appendStep(p, s)))

    def addStepAt(pid: ProcessId, s: Step, pos: Int) = saveAndReturnProcess(pid)(p => Option(insertStep(p, s, pos)))

    def moveStepTo(pid: ProcessId, from: Int, to: Int) = saveAndReturnProcess(pid)(p => Option(moveStep(p, from, to)))

    def removeStepAt(pid: ProcessId, at: Int) = saveAndReturnProcess(pid) { p =>
      removeStep(p, at)((procId, sid) => Task.findByProcessId(procId).filter(t => t.stepId == sid))
    }

    private[this] def saveAndReturnProcess(pid: ProcessId)(f: Process => Option[Process]): Option[Process] =
      saveAndReturn[Process](Process.findById(pid))(f)
  }

  object TaskService {

    def create(pid: ProcessId, t: Task) = Process.findById(pid).flatMap { p =>
      addTaskToProcess(p, t).map { task =>
        Task.save(t)
        t
      }
    }

    def findById(tid: TaskId): Option[Task] = Task.findById(tid)

    def findByProcessId(pid: ProcessId): Seq[Task] = Task.findByProcessId(pid)

    def update(tid: TaskId, t: Task): Option[Task] = {
      // TODO....first locate task...do diff...then save and return data
      Task.save(t)
      Task.findById(tid)
    }

    def toStep(pid: ProcessId, tid: TaskId, to: StepId): Option[Task] =
      Process.findById(pid).flatMap(p =>
        saveAndReturnTask(tid)(t => moveTask(p, t, to).map { tm =>
          Task.save(t)
          t
        })
      )

    def toNextStep(pid: ProcessId, tid: TaskId): Option[Task] =
      Process.findById(pid).flatMap(p => saveAndReturnTask(tid)(t => moveToNext(p, t)))

    def toPreviousStep(pid: ProcessId, tid: TaskId): Option[Task] =
      Process.findById(pid).flatMap(p => saveAndReturnTask(tid)(t => moveToPrevious(p, t)))

    def complete(tid: TaskId) = {
      // TODO: Check validity of Task with respect to the Process and Step (return if invalid)
      // TODO: Mark the task as completed and move on to the next Step.
      // TODO: Generate tasks according to the new Step.
      ???
    }

    def reject(tid: TaskId) = {
      // TODO: Mark the task as rejected and move back to the previous Step.
      // TODO: Generate tasks according to the new Step (or maybe re-open the previous task?).
      ???
    }

    def delegate(tid: TaskId, toUser: UserId) = reassign(tid, toUser)

    private[this] def saveAndReturnTask(taskId: TaskId)(f: Task => Option[Task]): Option[Task] =
      saveAndReturn[Task](Task.findById(taskId))(f)

    def reassign(taskId: TaskId, userId: UserId): Option[Task] = ???

    //      assignTask(userId, taskId)(tid => Task.findById(tid))
  }

}
