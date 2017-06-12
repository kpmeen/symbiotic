package services.party

import java.util.UUID

import models.base.SymbioticUserId
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import repository.mongodb.party.MongoDBAvatarRepository
import util.ExtendedMongoSpec

import scala.concurrent.ExecutionContext.Implicits.global

class AvatarServiceSpec extends ExtendedMongoSpec {

  lazy val repo    = new MongoDBAvatarRepository(extConfiguration)
  lazy val service = new AvatarService(repo)

  def addAndValidate[Id <: UserId](uid: Id, fileName: String) = {
    val fis = Option(getClass.getResourceAsStream(fileName))
    fis must not be empty

    val a = Avatar(
      uid = uid,
      ctype = Some("image/jpeg"),
      s = fis
    )

    val res = service.save(a).futureValue
    res must not be empty
    res.get mustBe an[UUID]
  }

  "When using the AvatarService it" should {
    "be possible to save a new Avatar" in {
      addAndValidate(
        SymbioticUserId.create(),
        "/testdata/images/han_solo.jpg"
      )
    }

    "be possible to get an Avatar" in {
      val uid = SymbioticUserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")

      val res = service.get(uid).futureValue
      res must not be empty
      res.get.filename mustBe uid.value
    }

    "be possible to remove an Avatar" in {
      val uid = SymbioticUserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")

      service.remove(uid).futureValue

      service.get(uid).futureValue mustBe None
    }

    "be possible to replace an Avatar" in {
      val uid = SymbioticUserId.create()
      addAndValidate(uid, "/testdata/images/han_solo.jpg")
      addAndValidate(uid, "/testdata/images/darth_vader.jpg")
    }
  }

}
