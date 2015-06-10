/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package hipe.core.eventstore

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import hipe.core.TaskId
import hipe.core.eventstore.HIPECommands.{HIPECommand, TaskCommand}

/**
 * Supervisor actor for the HIPE EventStore. It acts as the mediator
 * for all event messages to actors that operate in the context of
 * an event sourced data type.
 */
class HIPESupervisor extends Actor with ActorLogging {

  override def receive: Receive = {
    case TaskCommand(tid, cmd) => taskProcessor(tid) ! cmd
    case cmd: HIPECommand => taskProcessor(cmd.id.asInstanceOf[TaskId]) ! cmd
  }

  /**
   * Looks up the TaskProcessor actor representing the given TaskId
   */
  def taskProcessor(tid: TaskId): ActorRef =
    context.child(s"task-processor-${tid.value}").getOrElse(context.actorOf(Props(new TaskProcessor(tid))))
}
