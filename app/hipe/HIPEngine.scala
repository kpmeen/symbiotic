/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core._

object HIPEngine extends ProcessOperations {

  /**
   *
   * @param name
   * @param strict
   * @param description
   * @return
   */
  def makeProcess(name: String, strict: Boolean, description: Option[String]): Option[Process] = {
    val p = createProcess(name, strict, description)
    Process.save(p)
    Process.findById(p.id.get)
  }

  /**
   *
   * @param procId
   * @param f
   * @return
   */
  private[this] def applyAndSaveStep(procId: ProcessId)(f: Process => Option[Process]): Option[Process] =
    Process.findById(procId).flatMap { p =>
      f(p).map { pa =>
        Process.save(pa)
        pa
      }
    }

  /**
   *
   * @param procId
   * @param s
   * @return
   */
  def addStep(procId: ProcessId, s: Step): Option[Process] = applyAndSaveStep(procId)(p => Option(appendStep(p, s)))

  /**
   *
   * @param procId
   * @param s
   * @param idx
   * @return
   */
  def addStepAt(procId: ProcessId, s: Step, idx: Int): Option[Process] =
    applyAndSaveStep(procId)(p => Option(insertStep(p, s, idx)))

  /**
   *
   * @param procId
   * @param currStepPos
   * @param newStepPos
   * @return
   */
  def moveStepTo(procId: ProcessId, currStepPos: Int, newStepPos: Int): Option[Process] =
    applyAndSaveStep(procId)(p => Option(moveStep(p, currStepPos, newStepPos)))

  /**
   *
   * @param procId
   * @param stepIdx
   * @return
   */
  def removeStepFrom(procId: ProcessId, stepIdx: Int): Option[Process] =
    applyAndSaveStep(procId) { p =>
      removeStep(p, stepIdx)((pid, sid) => Task.findByProcessId(pid).filter(t => t.stepId == sid))
    }

  /**
   *
   * @param procId
   * @param taskId
   * @param newStepId
   * @return
   */
  def moveTaskTo(procId: ProcessId, taskId: TaskId, newStepId: StepId): Option[Task] =
    Process.findById(procId).flatMap { p =>
      Task.findById(taskId).flatMap { t =>
        moveTask(p, t, newStepId).map { t =>
          Task.save(t)
          t
        }
      }
    }

  /**
   *
   * @param procId
   * @param task
   * @return
   */
  def addTask(procId: ProcessId, task: Task): Option[Task] =
    Process.findById(procId).flatMap { p =>
      addTaskToProcess(p, task).map { t =>
        Task.save(t)
        t
      }
    }

}
