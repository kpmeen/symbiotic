/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.hipe.engine

import com.mongodb.casbah.TypeImports.ObjectId

/**
 * This case class holds actual process configuration.
 *
 * @param id ProcessId The unique identifier for the Process
 * @param name String with a readable name
 * @param strict Boolean flag indicating if movement of tasks in the process should be free-form/open or restricted
 * @param description String Readable text describing the process
 * @param steps List of Steps in the process.
 */
case class Process(
  id: ProcessId,
  name: String,
  strict: Boolean = false,
  description: Option[String],
  steps: List[_ <: Step] = List.empty) {

  /**
   * Appends a Step to the Process.
   */
  def appendStep[A <: Step](step: A): Process = this.copy(steps = steps ::: List(step))

  /**
   * Inserts a Step on the board at the defined index. If the index is larger than the current number of steps, the
   * Step is appended to the Process. If not it will be added at the given index, shifting tailing steps to the right.
   *
   * @param step the Step to insert
   * @param index the position to insert the Step in the list of steps
   * @return a Process with the new Step added to the list of steps
   */
  def insertStep[A <: Step](step: A, index: Int): Process = {
    if (index > steps.length) {
      appendStep(step)
    } else {
      val lr = steps.splitAt(index)
      this.copy(steps = lr._1 ::: List(step) ::: lr._2)
    }
  }

  /**
   * Allows for re-arranging steps in the process...
   *
   * currIndex > newIndex: the Step is moved `before` its current location
   * currIndex < newIndex: the Step is moved `after` its current location
   * currIndex == newIndex: the steps are left alone
   *
   * @param currIndex the current index position of the Step
   * @param newIndex the new index position to place the Step
   * @return A Process with an updated step order
   */
  def moveStep(currIndex: Int, newIndex: Int): Process = {
    if (currIndex == newIndex) {
      this
    } else {
      val index = if (newIndex >= steps.length) steps.length - 1 else newIndex
      val lr = steps.splitAt(currIndex)
      val removed = (lr._1 ::: lr._2.tail).splitAt(index)

      this.copy(steps = removed._1 ::: List(steps(currIndex)) ::: removed._2)
    }
  }

  /**
   * Removes the Step at the given index if the index number is lower or equal to the number of steps, and if the
   * Step to be removed does not contain any Tasks.
   *
   * @param stepIndex the step index to remove
   * @param findTasks function to identify which tasks belong to the given stepId on the given processId.
   * @return Some[Process] if the Step was removed, otherwise None
   */
  def removeStep(stepIndex: Int)(findTasks: (ProcessId, StepId) => List[Task]): Option[Process] = {
    if (stepIndex < steps.length) {
      if (steps.isDefinedAt(stepIndex)) {
        // Locate any tasks that are associated with the given step.
        val tasks = findTasks(id, steps(stepIndex).id)
        if (tasks.isEmpty) {
          val lr = steps.splitAt(stepIndex)
          return Some(this.copy(steps = lr._1 ::: lr._2.tail))
        }
      }
    }
    None
  }

  /**
   * Calculates the surroundings for the current Step
   *
   * @param currStep the current StepId
   * @return a type of PrevNextStepType that may or may not have previous and/or next Step references.
   */
  private[hipe] def prevNextSteps(currStep: StepId): PrevNextStepType = {
    val currIndex = steps.indexWhere(_.id == currStep)

    if (currIndex == 0) {
      NextOnlyStep(steps(1).id)
    } else if (currIndex == steps.length - 1) {
      PrevOnlyStep(steps(steps.length - 2).id)
    } else {
      PrevNextStep(steps(currIndex - 1).id, steps(currIndex + 1).id)
    }
  }
}

object Process {
  /**
   * Creates a new Process instance ready to be have steps added.
   *
   * @param name of the Process
   * @param strict flat indicating if the process should adhere to strict rules or not
   * @param desc an optional description of the process
   * @return the new Process instance
   */
  def create(name: String, strict: Boolean = false, desc: Option[String]): Process =
    Process(
      id = ProcessId(new ObjectId()),
      name = name,
      strict = strict,
      description = desc
    )
}