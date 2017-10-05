package net.scalytica.symbiotic.api.functional

import scala.concurrent.{ExecutionContext, Future}
import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.functional.MonadTransformers.SymResT

import scala.annotation.implicitNotFound

/**
 * Typeclass defining a simple Monad
 */
trait Monad[T[_]] {

  def map[A, B](value: T[A])(f: A => B): T[B]

  def flatMap[A, B](value: T[A])(f: A => T[B]): T[B]

  def pure[A](x: A): T[A]

}

object MonadTransformers {

  /**
   * Defines a Monad transformer for the SymRes type.
   */
  case class SymResT[T[_], A](value: T[SymRes[A]])(implicit m: Monad[T]) {

    def map[B](f: A => B): SymResT[T, B] =
      SymResT[T, B](m.map(value)(_.map(f)))

    def flatMap[B](f: A => SymResT[T, B]): SymResT[T, B] = {
      val res: T[SymRes[B]] = m.flatMap(value) { a =>
        a.map(b => f(b).value).getOrElse {
          a match {
            case err: Ko =>
              m.pure(err)

            case o =>
              m.pure(
                Failed(s"Unable to map into the result value because of $o")
              )
          }
        }
      }
      SymResT[T, B](res)
    }

  }

  object SymResT {

    def evaluated[A](
        res: SymRes[A]
    )(implicit m: Monad[Future]): SymResT[Future, A] = {
      SymResT(Future.successful(res))
    }

    def mapEvaluated[A](
        res: A
    )(implicit m: Monad[Future]): SymResT[Future, A] = {
      SymResT(Future.successful[SymRes[A]](Ok(res)))
    }

    def evaluatedFailed[A](
        ko: Ko
    )(implicit m: Monad[Future]): SymResT[Future, A] = {
      SymResT(Future.successful[SymRes[A]](ko))
    }

    def mapSequenceF[A](seq: Seq[Future[A]])(
        implicit m: Monad[Future],
        ec: ExecutionContext
    ): SymResT[Future, Seq[A]] = {
      SymResT(Future.sequence(seq).map[SymRes[Seq[A]]](Ok.apply))
    }

    def sequenceF[A](seq: Seq[Future[SymRes[A]]])(
        implicit m: Monad[Future],
        ec: ExecutionContext
    ): SymResT[Future, Seq[A]] = {
      SymResT(
        Future.sequence(seq).map { res =>
          res
            .find(_.failed)
            .map {
              case err: Ko => err
              case bad =>
                throw new IllegalStateException(
                  s"Found an Ok in the failed branch of execution. Got: $bad"
                )
            }
            .getOrElse {
              Ok(res.map {
                case Ok(value) => value
                case ko =>
                  throw new IllegalStateException(
                    s"Found a Ko in the success branch of execution. Got: $ko"
                  )
              })
            }
        }
      )
    }

  }
}

object Implicits {

  implicit def futSymRes_to_SymResT[A](
      sr: Future[SymRes[A]]
  )(implicit m: Monad[Future]): SymResT[Future, A] = SymResT(sr)

  implicit def symResT_to_FutSymRes[A](
      srt: SymResT[Future, A]
  ): Future[SymRes[A]] = srt.value

  /**
   * Implicit converter to wrap a
   * [[net.scalytica.symbiotic.api.functional.Monad]] around a
   * [[scala.concurrent.Future]]. This allows for composition of Monads using
   * Monad transformers.
   *
   * @param ec The ExecutionContext for mapping on the Future type
   * @return a Monad of type Future
   */
  @implicitNotFound(
    "A Future[Monad] needs an implicit ExecutionContext in scope. If you " +
      "are using the Play! Framework, consider using the frameworks default " +
      "context:\n\n" +
      "  import play.api.libs.concurrent.Execution.Implicits.defaultContext\n\n" +
      "Otherwise, use the ExecutionContext that best suits your needs."
  )
  implicit def futureMonad(implicit ec: ExecutionContext): Monad[Future] =
    new Monad[Future] {
      override def map[A, B](value: Future[A])(f: (A) => B) = value.map(f)

      override def flatMap[A, B](value: Future[A])(f: (A) => Future[B]) =
        value.flatMap(f)

      override def pure[A](x: A): Future[A] = Future(x)
    }

}
