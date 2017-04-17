package services.party

import java.util.UUID

import models.party.{Avatar, SymbioticUserId}
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.specs2.mutable.Specification
import repository.mongodb.party.MongoDBAvatarRepository
import net.scalytica.symbiotic.test.MongoSpec

class AvatarServiceSpec extends Specification with MongoSpec {

  val service = new MongoDBAvatarRepository(configuration)

  def addAndValidate[Id <: UserId](uid: Id, fileName: String) = {
    val fis = getClass.getResourceAsStream(fileName)
    fis must not beNull

    val a = Avatar(
      uid = uid,
      ctype = Some("image/jpeg"),
      s = Option(fis)
    )

    val res = service.save(a)
    res must_!= None
    res.get.getClass must_== classOf[UUID]
  }

  "When using the AvatarService it" should {
    "be possible to save a new Avatar" in {
      addAndValidate(SymbioticUserId.create(), "/testdata/images/han_solo.jpg")
    }

    "be possible to get an Avatar" in {
      val uid = SymbioticUserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")

      val res = service.get(uid)
      res must_!= None
      res.get.filename must_== uid.value
    }

    "be possible to remove an Avatar" in {
      val uid = SymbioticUserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")

      service.remove(uid)

      val res = service.get(uid)
      res must beNone
    }

    "be possible to replace an Avatar" in {
      val uid = SymbioticUserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")
      addAndValidate(uid, "/testdata/images/darth_vader.jpg")
    }
  }

}
