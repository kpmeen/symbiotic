package net.scalytica.symbiotic.test.utils

import scala.concurrent._
import scala.concurrent.duration._

object DelayedExecution {

  def delay[T](
      exec: () => T,
      waitFor: FiniteDuration
  )(implicit ec: ExecutionContext): T = {
    val deadline = waitFor.fromNow
    val dlv =
      new DelayedLazyVal(() => deadline.timeLeft.max(Duration.Zero), blocking {
        Thread.sleep(deadline.timeLeft.toMillis)
      })

    while (!dlv.isDone) {}

    exec()
  }

}
