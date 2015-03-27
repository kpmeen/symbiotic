/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.hipe

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
  steps: List[_ <: Step] = List.empty)