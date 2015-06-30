/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe

import hipe.core.FailureTypes._
import hipe.core._
import org.slf4j.LoggerFactory

/**
 * Functions that perform computations on Process data
 */
private[hipe] trait ProcessOperations {

  private val logger = LoggerFactory.getLogger(classOf[ProcessOperations])

  /**
   * Appends a Step at the end of the process in a new separate StepGroup.
   *
   * @param proc the Process to append a step to
   * @param step the Step to append
   * @return the Process with the appended Step
   */
  protected def appendStep(proc: Process, step: Step): Process = appendGroup(proc, StepGroup.create(step))

  /**
   * Appends a Step group at the end of the list of groups in the process.
   *
   * @param proc the Process to append a group to
   * @param sg the StepGroup to append
   * @return the Process with the appended StepGroup
   */
  protected def appendGroup(proc: Process, sg: StepGroup): Process =
    proc.copy(stepGroups = proc.stepGroups ::: StepGroupList(sg))

  /**
   * Will append a Step to the end of the specified StepGroup.
   *
   * @param proc the Process
   * @param sgid the StepGroupId where the Step should be appended
   * @param step the Step to append
   * @return HIPEResult with the updated Process config
   */
  protected def appendStepToGroup(proc: Process, sgid: StepGroupId, step: Step): HIPEResult[Process] =
    proc.stepGroups.findWithPosition(sgid).map {
      case (sg: StepGroup, idx: Int) =>
        val nsg = sg.copy(steps = sg.steps ::: StepList(step))
        Right(proc.copy(stepGroups = proc.stepGroups.updated(idx, nsg)))
    }.getOrElse(Left(NotFound(s"Could not find StepGroup $sgid")))

  /**
   * Adds a Step in a new StepGroup in the process at the defined position.
   * The position is calculated from a flattened view of the steps in all
   * the step groups. If the index is larger than the current number of steps,
   * the Step is appended to the Process. If not it will be added at the given
   * index, shifting tailing steps to the right.
   *
   * @param proc the Process
   * @param step the Step to insert
   * @param pos the position to insert the Step in the list of steps
   * @return a Process with the new Step added to the list of steps
   */
  protected def insertStep(proc: Process, step: Step, pos: Int): Process =
    if (pos > proc.stepGroups.length) appendStep(proc, step)
    else proc.copy(stepGroups = proc.stepGroups.insert(StepGroup.create(step), pos))

  /**
   * Adds a Step to the specified StepGroup at the defined position. The position is
   * calculated based on size of the StepGroup. If the pos is larger than the number
   * steps in the group, the Step is appended to the Process. If not it will be added
   * at the given index, shifting tailing steps to the right.
   *
   * @param proc the Process
   * @param sgid the StepGroup to add a step to
   * @param step the Step to insert
   * @param pos the position in the StepGroup to insert the step
   * @return HIPEResult with the updated Process config
   */
  protected def insertStepToGroup(proc: Process, sgid: StepGroupId, step: Step, pos: Int): HIPEResult[Process] =
    proc.stepGroups.findWithPosition(sgid).map {
      case (sg: StepGroup, idx: Int) =>
        if (pos > sg.steps.length) {
          appendStepToGroup(proc, sgid, step)
        } else {
          val nsg = sg.copy(steps = sg.steps.insert(step, pos))
          Right(proc.copy(stepGroups = proc.stepGroups.updated(idx, nsg)))
        }
    }.getOrElse(Left(NotFound(s"Could not find StepGroup $sgid")))

  /**
   * Allows for re-arranging groups of steps in the process...
   *
   * currPos > newPos: the StepGroup is moved `before` its current location
   * currPos < newPos: the StepGroup is moved `after` its current location
   * currPos == newPos: nothing is changed
   *
   * @param proc the Process
   * @param sgid the StepGroup to move
   * @param newPos the new index position to place the StepGroup
   * @return HIPEResult with the updated process config
   */
  protected def moveStepGroup(proc: Process, sgid: StepGroupId, newPos: Int): HIPEResult[Process] =
    proc.stepGroups.findWithPosition(sgid).map {
      case (group: StepGroup, currPos: Int) =>
        if (currPos == newPos) Left(NotPossible(s"Old ($currPos) and new ($newPos) positions are the same..."))
        else Right(proc.copy(stepGroups = proc.stepGroups.move(currPos, newPos)))
    }.getOrElse(Left(NotFound(s"Could not find StepGroup $sgid")))

  /**
   * Allows for re-arranging steps inside their enclosing StepGroup.
   *
   * currPos > newPos: the Step is moved `before` its current location
   * currPos < newPos: the Step is moved `after` its current location
   * currPos == newPos: nothing is changed
   *
   * @param proc the Process
   * @param sgid the StepGroupId containing the step to move
   * @param sid the StepId to move
   * @param newPos the new position of the Step relative to the enclosing StepGroup
   * @return HIPEResult with the updated process config
   */
  protected def moveStepInGroup(proc: Process, sgid: StepGroupId, sid: StepId, newPos: Int): HIPEResult[Process] =
    proc.stepGroups.findWithPosition(sgid).map {
      case (group: StepGroup, grpPos: Int) => group.steps.findWithPosition(sid).map {
        case (step: Step, currPos: Int) =>
          if (currPos == newPos) {
            Left(NotPossible(s"Old ($currPos) and new ($newPos) positions are the same..."))
          } else {
            val nsg = group.copy(steps = group.steps.move(currPos, newPos))
            Right(proc.copy(stepGroups = proc.stepGroups.updated(grpPos, nsg)))
          }
      }.getOrElse(Left(NotFound(s"Could not find Step: $sid")))
    }.getOrElse(Left(NotFound(s"Could not find StepGroup: $sgid")))

  /**
   * Moves a Step out of its current StepGroup into the given position in the target StepGroup.
   *
   * @param proc the Process
   * @param stepId the Id of the step to move
   * @param toSgid the StepGroup to move to
   * @param posInGrp the position in the target StepGroup to move the step to
   * @return HIPEResult with the updated Process config
   */
  protected def moveStepToGroup(proc: Process, stepId: StepId, toSgid: StepGroupId, posInGrp: Int): HIPEResult[Process] =
    proc.stepGroups.findStep(stepId).map {
      case (group: StepGroup, step: Step) =>
        val p1 = proc.removeStep(group, step)
        insertStepToGroup(p1, toSgid, step, posInGrp)
    }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))

  /**
   * Will move a Step away from the enclosing StepGroup, and into a brand new StepGroup
   * at the given position in the process. If the original StepGroup is empty after the
   * move it will be removed from the process.
   *
   * @param proc the Process
   * @param stepId the StepIdId to move
   * @param newPos the position in the process where the new StepGroup will be added.
   * @return HIPEResult with the updated Process config
   */
  protected def moveStepToNewGroup(proc: Process, stepId: StepId, newPos: Int): HIPEResult[Process] =
    proc.stepGroups.findStep(stepId).map {
      case (group: StepGroup, step: Step) =>
        val p1 = proc.removeStep(group, step)
        Right(insertStep(p1, step, newPos - 1))
    }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))

  /**
   * ¡¡¡ WARNING !!!
   *
   * Removes the Step with the given stepId if it exists. If the Step is the single entry in the
   * enclosing StepGroup, the StepGroup is also removed.
   *
   * @param proc the Process to remove a step from
   * @param stepId the stepId to remove
   * @return HIPEResult with the updated Process config
   */
  protected def removeStep(proc: Process, stepId: StepId): HIPEResult[Process] =
    proc.stepGroups.findStep(stepId).map {
      case (group: StepGroup, step: Step) => Right(proc.removeStep(group, step))
    }.getOrElse(Left(NotFound(s"Could not find step: $stepId")))

  /**
   * ¡¡¡ WARNING !!!
   *
   * Removes an entire StepGroup including the contained Step items.
   *
   * @param proc the Process
   * @param sgid the Id of the StepGroup to remove
   * @return
   */
  protected def removeGroup(proc: Process, sgid: StepGroupId): HIPEResult[Process] =
    proc.stepGroups.findWithPosition(sgid).map {
      case (group: StepGroup, pos: Int) => Right(proc.copy(stepGroups = proc.stepGroups.remove(pos)))
    }.getOrElse(Left(NotFound(s"Could not find group: $sgid")))
}
