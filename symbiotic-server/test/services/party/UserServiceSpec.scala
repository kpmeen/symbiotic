package services.party

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.{Created, Success, Updated}
import models.base.Gender.Male
import models.base.{Email, Name, SymbioticUserId, Username}
import models.party.User
import org.joda.time.DateTime
import repository.mongodb.party.MongoDBUserRepository
import util.ExtendedMongoSpec

import scala.concurrent.ExecutionContext.Implicits.global

class UserServiceSpec extends ExtendedMongoSpec {

  lazy val repo    = new MongoDBUserRepository(extConfiguration)
  lazy val service = new UserService()(global, repo)

  def buildUser(uname: Username, email: Email, name: Name): User =
    User(
      id = SymbioticUserId.createOpt(),
      loginInfo = LoginInfo(CredentialsProvider.ID, uname.value),
      v = None,
      username = uname,
      email = email,
      name = Some(name),
      dateOfBirth = Some(DateTime.now().minusYears(20)), // scalastyle:ignore
      gender = Some(Male())
    )

  def saveAndValidate[A <: Success](usr: User, s: A) =
    service.save(usr).futureValue mustBe s

  "When using the UserService it" should {
    "be possible to add a new User" in {
      val usr = buildUser(
        Username("foobar"),
        Email("foobar@fizzbuzz.no"),
        Name(Some("foo"), None, Some("bar"))
      )
      saveAndValidate(usr, Created)
    }

    "be possible to find a User by UserId" in {
      val usr = buildUser(
        Username("fiifaa"),
        Email("fiifaa@fizzbuzz.no"),
        Name(Some("fii"), None, Some("faa"))
      )
      saveAndValidate(usr, Created)

      val res = service.findById(usr.id.get).futureValue
      res must not be empty
      res.get.name mustBe usr.name
      res.get.email mustBe usr.email
      res.get.username mustBe usr.username
    }

    "be possible to find a User by Username" in {
      val usr = buildUser(
        Username("boobaa"),
        Email("boobaa@fizzbuzz.no"),
        Name(Some("boo"), None, Some("baa"))
      )
      saveAndValidate(usr, Created)

      val res = service.findByUsername(usr.username).futureValue
      res must not be empty
      res.get.name mustBe usr.name
      res.get.email mustBe usr.email
      res.get.username mustBe usr.username
    }

    "be possible to update a User" in {
      val usr = buildUser(
        Username("liiloo"),
        Email("liiloo@fizzbuzz.no"),
        Name(Some("lii"), None, Some("loo"))
      )
      saveAndValidate(usr, Created)

      val res1 = service.findById(usr.id.get).futureValue
      res1 must not be empty

      val mod =
        res1.get.copy(name = res1.get.name.map(_.copy(middle = Some("laa"))))
      saveAndValidate(mod, Updated)

      val res2 = service.findById(usr.id.get).futureValue
      res2 must not be empty
      res2.get.name mustBe mod.name
      res2.get.email mustBe usr.email
      res2.get.username mustBe usr.username
    }
  }

}
