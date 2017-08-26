package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}

// scalastyle:off magic.number
class CustomMetadataAttributesSpec extends WordSpec with MustMatchers {

  val now = DateTime.now()
  val originalMap = Map[String, Any](
    "foo"     -> "bar",
    "fizz"    -> 12,
    "buzz"    -> true,
    "baz"     -> 14L,
    "zippedi" -> 22.22,
    "doodaa"  -> now
  )

  "CustomMetadataAttributes" should {

    "implicitly convert a map to a MetadataMap" in {
      val map: MetadataMap = originalMap

      map mustBe a[MetadataMap]
      map.getAs[String]("foo") mustBe Some("bar")
      map.getAs[Int]("fizz") mustBe Some(12)
      map.getAs[Boolean]("buzz") mustBe Some(true)
      map.getAs[Long]("baz") mustBe Some(14L)
      map.getAs[Double]("zippedi") mustBe Some(22.22)
      map.getAs[DateTime]("doodaa") mustBe Some(now)
    }

    "implicitly convert a custom value class to a MetadataValue" in {
      case class Foo(bar: String)
      case class FooValue(value: Foo) extends MetadataValue[Foo]

      implicit val fooConverter: Converter[Foo] = Converter(FooValue.apply)

      val fooMd: MetadataValue[Foo] = Foo("bar")

      fooMd mustBe a[MetadataValue[_]]
      fooMd.value mustBe Foo("bar")
    }

    "implicitly unwrap a MetadataValue to an Option of correct data type" in {
      val mdv: MetadataValue[String] = StrValue("foobar")

      val maybeStr: Option[String] = mdv

      maybeStr mustBe Some("foobar")
    }

    "be able to return the correct type when doing getAs[T](key)" in {
      originalMap.getAs[Boolean]("buzz") mustBe Some(true)
    }

    "combine MetadataMaps" in {
      val opt1: Option[String] = Option("baz")
      val opt2: Option[String] = None

      val md1              = MetadataMap("a" -> 12, "b" -> "foobar")
      val md2              = MetadataMap("c" -> true, "d" -> opt1, "e" -> opt2)
      val md3: MetadataMap = md1 ++ md2

      md3.keys must contain allOf ("a", "b", "c", "d", "e")

      md3.getAs[Int]("a") mustBe Some(12)
      md3.getAs[String]("b") mustBe Some("foobar")
      md3.getAs[Boolean]("c") mustBe Some(true)
      md3.getAs[String]("d") mustBe Some("baz")
      md3.getAs[String]("e") mustBe None
    }

  }
}
// scalastyle:on magic.number
