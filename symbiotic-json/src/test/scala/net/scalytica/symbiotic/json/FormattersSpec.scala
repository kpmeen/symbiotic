package net.scalytica.symbiotic.json

import java.util.UUID

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.json.Implicits._
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.Inside._
import play.api.libs.json._

class FormattersSpec extends WordSpec with MustMatchers {

  val id  = UUIDGenerator.generate()
  val uid = UserId.create()
  val fid = FileId.create()

  val now   = DateTime.now
  val nowJs = Json.toJson(now)

  val lock = Lock(uid, now)
  val path = Path("/root/foo/bar/baz/")

  val md = ManagedMetadata(
    owner = Option(uid),
    fid = Option(fid),
    uploadedBy = Option(uid),
    isFolder = Some(true),
    path = Option(path),
    description = Option("This is a description"),
    lock = Option(lock)
  )

  // scalastyle:off
  // format: off
  val extraAttribs = MetadataMap(
    "foo"  -> "bar",
    "fizz" -> 12,
    "buzz" -> 33.33,
    "fi"   -> false,
    "fa"   -> true,
    "fum"  -> new DateTime(2017, 7, 11, 9, 6, 17, 430, DateTimeZone.UTC)
  )
  // format: on
  // scalastyle:on

  val f: ManagedFile = Folder(
    id = Option(id),
    filename = "FooBar",
    metadata = md
  )

  "Formatting the Lock type" should {
    "serialize a Lock instance to JSON" in {
      val js = Json.toJson[Lock](lock)

      val by = (js \ "by").as[UserId]
      by mustBe uid

      (js \ "date").as[DateTime] mustBe now
    }

    "deserialize JSON to a Lock" in {
      val js =
        s"""{
           |  "by" : "${uid.value}",
           |  "date" : "${nowJs.as[String]}"
           |}
         """.stripMargin

      val lockRes = Json.fromJson[Lock](Json.parse(js))

      lockRes.isSuccess mustBe true
      lockRes.get.by mustBe lock.by
      lockRes.get.date mustBe lock.date
    }
  }

  "Formatting the UserStamp type" should {
    "serialize a UserStamp instance to JSON" in {
      val us = UserStamp(now, uid)

      val js = Json.toJson(us)

      (js \ "by").as[UserId] mustBe uid
      (js \ "date").as[DateTime] mustBe now
    }

    "deserialize JSON to a UserStamp" in {
      pending
    }
  }

  "Formatting the Folder type" should {
    "serialize a Folder instance to JSON" in {
      val js = Json.toJson(f)

      (js \ "id").as[UUID] mustBe id
      (js \ "filename").as[String] mustBe "FooBar"
      (js \ "metadata" \ "owner").asOpt[UserId] mustBe md.owner
      (js \ "metadata" \ "fid").asOpt[FileId] mustBe md.fid
    }

    "deserialize JSON to a Folder" in {
      val js =
        s"""{
           |  "id" : "${id.toString}",
           |  "filename" : "FooBar",
           |  "metadata" : {
           |    "owner" : "${uid.value}",
           |    "fid" : "${fid.value}",
           |    "uploadedBy" : "${uid.value}",
           |    "version" : 1,
           |    "isFolder" : true,
           |    "path" : "/root/foo/bar/baz",
           |    "description" : "This is a description",
           |    "lock" : {
           |      "by" : "${uid.value}",
           |      "date" : "${nowJs.as[String]}"
           |    }
           |  }
           |}
         """.stripMargin

      val folder = Json.fromJson[ManagedFile](Json.parse(js))
      folder.isSuccess mustBe true
      folder.get mustBe f
    }

    "deserialize JSON with extra metadata attributes to a Folder" in {
      val js =
        s"""{
           |  "id" : "${id.toString}",
           |  "filename" : "FooBar",
           |  "metadata" : {
           |    "owner" : "${uid.value}",
           |    "fid" : "${fid.value}",
           |    "uploadedBy" : "${uid.value}",
           |    "version" : 1,
           |    "isFolder" : true,
           |    "path" : "/root/foo/bar/baz",
           |    "description" : "This is a description",
           |    "lock" : {
           |      "by" : "${uid.value}",
           |      "date" : "${nowJs.as[String]}"
           |    },
           |    "extraAttributes": {
           |      "foo": "bar",
           |      "fizz": 12,
           |      "buzz": 33.33,
           |      "fi": false,
           |      "fa": true,
           |      "fum": "2017-07-11T11:06:17.430+02:00"
           |    }
           |  }
           |}
         """.stripMargin

      val folder = Json.fromJson[ManagedFile](Json.parse(js))
      folder.isSuccess mustBe true
      val mea = folder.get.metadata.extraAttributes
      mea must not be empty
      inside(mea) {
        case Some(mm) =>
          mm.get("foo") mustBe extraAttribs.get("foo")
          mm.get("fizz") mustBe extraAttribs.get("fizz")
          mm.get("buzz") mustBe extraAttribs.get("buzz")
          mm.get("fi") mustBe extraAttribs.get("fi")
          mm.get("fa") mustBe extraAttribs.get("fa")
          // scalastyle:off
          // format: off
          // Dates are compared by calling toString. Quick and dirty similarity.
          mm.get("fum").map(_.value.toString) mustBe extraAttribs.get("fum").map(_.value.toString)
        // scalastyle:on
        // format: on

        case None =>
          fail("Expected a MetadataMap")
      }
    }
  }

}
