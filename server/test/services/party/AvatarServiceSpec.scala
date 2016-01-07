/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import models.party.Avatar
import models.party.PartyBaseTypes.UserId
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import repository.mongodb.party.MongoDBAvatarRepository
import util.mongodb.MongoSpec

class AvatarServiceSpec extends Specification with MongoSpec {

  val service = new MongoDBAvatarRepository()

  def addAndValidate(uid: UserId, fileName: String) = {
    val fis = getClass.getResourceAsStream(fileName)
    fis must not beNull

    val a = Avatar(
      uid,
      Some("image/jpeg"),
      Option(fis)
    )

    val res = service.save(a)
    res must_!= None
    res.get.getClass must_== classOf[ObjectId] // scalastyle:ignore
  }

  "When using the AvatarService it" should {
    "be possible to save a new Avatar" in {
      addAndValidate(UserId.create(), "/testdata/images/han_solo.jpg")
    }

    "be possible to get an Avatar" in {
      val uid = UserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")

      val res = service.get(uid)
      res must_!= None
      res.get.filename must_== uid.value // scalastyle:ignore
    }

    "be possible to remove an Avatar" in {
      val uid = UserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")

      service.remove(uid)

      val res = service.get(uid)
      res must_== None
    }

    "be possible to replace an Avatar" in {
      val uid = UserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")
      addAndValidate(uid, "/testdata/images/darth_vader.jpg")
    }
  }

}
