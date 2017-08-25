package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.{
  Converter,
  MetadataMap,
  MetadataValue,
  StrValue
}
import CustomMetadataAttributes.Implicits._
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}

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
      map("foo").value mustBe "bar"
      map("fizz").value mustBe 12
      map("buzz").value mustBe true
      map("baz").value mustBe 14L
      map("zippedi").value mustBe 22.22
      map("doodaa").value mustBe now
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

  }

}
