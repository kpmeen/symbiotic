package net.scalytica.symbiotic.play

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.play.json.Implicits._
import net.scalytica.symbiotic.test.TestUserId
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.libs.json._

class FormattersSpec extends Specification {

  implicit val testUserIdFormat: Format[UserId] = Format(
    fjs = Reads(_.validate[String].map[UserId](TestUserId.apply)),
    tjs = Writes(id => JsString(id.value))
  )

  val id  = UUIDGenerator.generate()
  val uid = TestUserId.create()
  val fid = FileId.create()

  val now   = DateTime.now
  val nowJs = Json.toJson(now)

  val lock = Lock(uid, now)
  val path = Path("/root/foo/bar/baz/")

  val md = ManagedFileMetadata(
    owner = Option(uid),
    fid = Option(fid),
    uploadedBy = Option(uid),
    isFolder = Some(true),
    path = Option(path),
    description = Option("This is a description"),
    lock = Option(lock)
  )

  val f: ManagedFile = Folder(
    id = Option(id),
    filename = "FooBar",
    metadata = md
  )

  "Formatting the Lock type" should {
    "serialize a Lock instance to JSON" in {
      val js = Json.toJson[Lock](lock)

      (js \ "by").as[UserId] must_== uid
      (js \ "date").as[DateTime].withTimeAtStartOfDay() must_== now
        .withTimeAtStartOfDay()
    }

    "deserialize JSON to a Lock" in {
      val js =
        s"""{
           |  "by" : "${uid.value}",
           |  "date" : "${nowJs.as[String]}"
           |}
         """.stripMargin

      val lockRes = Json.fromJson[Lock](Json.parse(js))

      lockRes.isSuccess must_== true
      lockRes.get.by must_== lock.by
      lockRes.get.date.withTimeAtStartOfDay() must_== lock.date
        .withTimeAtStartOfDay()
    }
  }

  "Formatting the UserStamp type" should {
    "serialize a UserStamp instance to JSON" in {
      val us = UserStamp(now, uid)

      val js = Json.toJson(us)

      (js \ "by").as[UserId] must_== uid
      (js \ "date").as[DateTime].dayOfYear() must_== now.dayOfYear()
    }

    "deserialize JSON to a UserStamp" in {
      pending
    }
  }

  "Formatting the Folder type" should {
    "serialize a Folder instance to JSON" in {
      val js = Json.toJson(f)

      (js \ "id").as[UUID] must_== id
      (js \ "filename").as[String] must_== "FooBar"
      (js \ "metadata" \ "owner").asOpt[UserId] must_== md.owner
      (js \ "metadata" \ "fid").asOpt[FileId] must_== md.fid
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

      folder.isSuccess must_== true
      folder.get must_== f
    }
  }

}
