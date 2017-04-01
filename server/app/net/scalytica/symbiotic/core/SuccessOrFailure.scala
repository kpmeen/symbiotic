/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.core

sealed trait SuccessOrFailure

abstract class Success extends SuccessOrFailure

case object Created extends Success

case object Updated extends Success

case object Removed extends Success

case class Failure(msg: String) extends SuccessOrFailure
