package net.scalytica.symbiotic.play

import java.util.UUID

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.play.json.Implicits._
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._

class FormattersSpec extends WordSpec with MustMatchers {

  val id  = UUIDGenerator.generate()
  val uid = UserId.create()
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
  }

}
