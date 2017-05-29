package services.party

import java.util.UUID

import models.base.SymbioticUserId
import models.party.Avatar
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.specs2.mutable.Specification
import repository.mongodb.party.MongoDBAvatarRepository
import net.scalytica.symbiotic.test.MongoSpec
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.mutable.ExecutionEnvironment

import scala.concurrent.Await
import scala.concurrent.duration._

class AvatarServiceSpec
    extends Specification
    with ExecutionEnvironment
    with MongoSpec {

  lazy val repo    = new MongoDBAvatarRepository(configuration)
  lazy val service = new AvatarService(repo)

  val timeout: Duration = 2 seconds

  def addAndValidate[Id <: UserId](
      uid: Id,
      fileName: String
  )(implicit ee: ExecutionEnv) = {
    val fis = getClass.getResourceAsStream(fileName)
    fis must not beNull

    val a = Avatar(
      uid = uid,
      ctype = Some("image/jpeg"),
      s = Option(fis)
    )

    val res = Await.result(service.save(a), timeout)
    res must_!= None
    res.get.getClass must_== classOf[UUID]
  }

  // scalastyle:off method.length
  def is(implicit ee: ExecutionEnv) = {

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

        val res = Await.result(service.get(uid), timeout)
        res must_!= None
        res.get.filename must_== uid.value
      }

      "be possible to remove an Avatar" in {
        val uid = SymbioticUserId.create()
        addAndValidate(uid, "/testdata/images/han_solo.jpg")

        service.remove(uid)

        val res = Await.result(service.get(uid), timeout)
        res must beNone
      }

      "be possible to replace an Avatar" in {
        val uid = SymbioticUserId.create()
        addAndValidate(uid, "/testdata/images/han_solo.jpg")
        addAndValidate(uid, "/testdata/images/darth_vader.jpg")
      }
    }
  }
  // scalastyle:on method.length

}
