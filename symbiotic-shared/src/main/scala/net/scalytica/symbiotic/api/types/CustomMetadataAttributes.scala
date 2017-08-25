package net.scalytica.symbiotic.api.types

import java.util.{Date => JDate}

import org.joda.time.DateTime

import scala.collection.MapLike

object CustomMetadataAttributes {

  /**
   * Representation of a typed metadata value.
   *
   * @tparam T the type of metadata value
   */
  trait MetadataValue[+T] {
    def value: T
  }

  final case class StrValue(value: String)    extends MetadataValue[String]
  final case class IntValue(value: Int)       extends MetadataValue[Int]
  final case class LongValue(value: Long)     extends MetadataValue[Long]
  final case class DoubleValue(value: Double) extends MetadataValue[Double]
  final case class BoolValue(value: Boolean)  extends MetadataValue[Boolean]
  final case class JodaValue(value: DateTime) extends MetadataValue[DateTime]

  /** Special case for metadata values with no actual value */
  final case object EmptyValue extends MetadataValue[Nothing] {
    override def value = throw new IllegalAccessException("Value is Nothing")
  }

  /**
   * Custom Map implementation that can store tuples of
   * String -> MetadataValue[_] where _ can be any supported impl of the
   * MetadataValue trait.
   *
   * @param underlying the underlying Map supporting the custom implementation.
   */
  final class MetadataMap(underlying: Map[String, MetadataValue[_]])
      extends Map[String, MetadataValue[_]]
      with MapLike[String, MetadataValue[_], MetadataMap] {

    override def empty = new MetadataMap(Map.empty[String, MetadataValue[_]])

    override def get(key: String) = underlying.get(key)

    def getAs[T](key: String)(implicit c: Converter[T]): Option[T] =
      get(key).flatMap {
        case EmptyValue => None
        case mdv        => Some(c.from(mdv.asInstanceOf[MetadataValue[T]]))
      }

    override def iterator = underlying.iterator

    override def +[B1 >: MetadataValue[_]](kv: (String, B1)) = underlying + kv

    override def -(key: String) = new MetadataMap(underlying - key)

    override def seq = underlying.seq

    override def stringPrefix: String = "MetadataMap"

    def plainMap: Map[String, Any] = {
      underlying.map {
        case (key, EmptyValue)          => key -> null // scalastyle:ignore
        case (key, v: MetadataValue[_]) => key -> v.value
      }
    }

  }

  object MetadataMap {

    val empty = new MetadataMap(Map.empty[String, MetadataValue[_]])

    // scalastyle:off null
    def apply(elems: (String, MetadataValue[_])*): MetadataMap = {
      new MetadataMap(elems.filterNot(_._2 == null).toMap)
    }

    // scalastyle:on null
  }

  /**
   * Defines a converter type-class that knows how to turn an A into a
   * MetadataValue[A]
   *
   * @tparam A the type to convert to a MetadataValue
   */
  trait Converter[A] {
    def to(arg: A): MetadataValue[A]

    def from(arg: MetadataValue[A]): A = arg.value
  }

  object Converter {

    def apply[A](f: A => MetadataValue[A]) = {
      new Converter[A] {
        override def to(arg: A) = f(arg)
      }
    }
  }

  /**
   * Default implicit Converter instances
   */
  trait DefaultConverters {

    implicit val strConv: Converter[String]    = Converter(StrValue.apply)
    implicit val intConv: Converter[Int]       = Converter(IntValue.apply)
    implicit val longConv: Converter[Long]     = Converter(LongValue.apply)
    implicit val doubleConv: Converter[Double] = Converter(DoubleValue.apply)
    implicit val boolConv: Converter[Boolean]  = Converter(BoolValue.apply)
    implicit val jodaConv: Converter[DateTime] = Converter(JodaValue.apply)

  }

  /**
   * Useful implicits to allow for more natural construction of MetadataMap
   * instances, using the normal Map construction like:
   * {{{
   *   val m = MetadataMap(
   *     "foo" -> "bar",
   *     "baz" -> None,
   *     "yak" -> Some("shaving"),
   *     "biz" -> DateTime.now,
   *     "fool" -> true,
   *     "fi" -> 12
   *   )
   * }}}
   */
  object Implicits extends DefaultConverters {
    implicit def convert[T](a: T)(implicit c: Converter[T]): MetadataValue[T] =
      c.to(a)

    implicit def convertNone(none: Option[Nothing]): MetadataValue[Nothing] =
      EmptyValue

    implicit def convertOpt[T](maybe: Option[T])(
        implicit c: Converter[T]
    ): MetadataValue[T] = maybe.map(c.to).getOrElse(EmptyValue)

    implicit def unwrapToOpt[T](a: MetadataValue[T])(
        implicit c: Converter[T]
    ): Option[T] = a match {
      case EmptyValue => None
      case _          => Some(c.from(a))
    }

    implicit def optUnwrapToOpt[T](oa: Option[MetadataValue[T]])(
        implicit c: Converter[T]
    ): Option[T] = oa.flatMap(a => unwrapToOpt(a))

    // scalastyle:off cyclomatic.complexity
    def anyConverter(a: Any): MetadataValue[_] = a match {
      case v: String      => convert[String](v)
      case v: Short       => convert[Int](v.toInt)
      case v: Int         => convert[Int](v)
      case v: Long        => convert[Long](v)
      case v: Double      => convert[Double](v)
      case v: Float       => convert[Double](v.toDouble)
      case v: Boolean     => convert[Boolean](v)
      case v: DateTime    => convert[DateTime](v)
      case v: JDate       => convert[DateTime](new DateTime(v.getTime))
      case opt: Option[_] => opt.map(anyConverter).getOrElse(EmptyValue)
      case null           => EmptyValue // scalastyle:ignore
      case v              => EmptyValue
    }

    implicit def convertAnyMap(m: Map[String, Any]): MetadataMap = {
      val c = m.map(t => t._1 -> anyConverter(t._2))
      MetadataMap(c.toSeq: _*)
    }

    // scalastyle:on cyclomatic.complexity
  }

}
