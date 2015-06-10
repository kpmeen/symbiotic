/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core.eventstore

import hipe.core.TaskId
import hipe.core.eventstore.TaskProtocol.Commands.TaskCmd
import models.base.Id

object HIPECommands {

  sealed trait HIPECommand {
    val id: Id
  }

  case class TaskCommand(id: TaskId, cmd: TaskCmd) extends HIPECommand

  case class Snapshot(id: Id) extends HIPECommand

  case class Print(id: Id) extends HIPECommand
}