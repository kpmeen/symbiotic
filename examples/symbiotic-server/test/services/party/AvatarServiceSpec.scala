package services.party

import java.util.UUID

import akka.stream.scaladsl.StreamConverters
import models.base.SymbioticUserId
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}

trait AvatarServiceSpec
    extends WordSpecLike
    with MustMatchers
    with ScalaFutures
    with OptionValues {

  def service: AvatarService

  def addAndValidate[Id <: UserId](uid: Id, fileName: String): Option[UUID] = {
    val fis = Option(
      StreamConverters.fromInputStream(
        () => getClass.getResourceAsStream(fileName)
      )
    )
    fis must not be empty

    val a = Avatar(
      uid = uid,
      ctype = Some("image/jpeg"),
      s = fis
    )

    val res = service.save(a).futureValue
    res must not be empty
    res.value mustBe an[UUID]

    res
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
      res.value.filename mustBe uid.value
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
