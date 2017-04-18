package services.party

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.base.Gender.Male
import models.base.{Email, Name, Username}
import models.party.{SymbioticUserId, User}
import net.scalytica.symbiotic.api.types.{Created, Success, Updated}
import net.scalytica.symbiotic.test.MongoSpec
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import repository.mongodb.party.MongoDBUserRepository

class UserServiceSpec
    extends Specification
    with WithUserService
    with MongoSpec {

  def buildUser(uname: Username, email: Email, name: Name): User =
    User(
      id = SymbioticUserId.createOpt(),
      loginInfo = LoginInfo(CredentialsProvider.ID, uname.value),
      v = None,
      username = uname,
      email = email,
      name = Some(name),
      dateOfBirth = Some(DateTime.now().minusYears(20)),
      gender = Some(Male())
    )

  def saveAndValidate[A <: Success](usr: User, s: A) =
    service.save(usr) must_== s

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

      val res = service.findById(usr.id.get)
      res must_!= None
      res.get.name must_== usr.name
      res.get.email must_== usr.email
      res.get.username must_== usr.username
    }

    "be possible to find a User by Username" in {
      val usr = buildUser(
        Username("boobaa"),
        Email("boobaa@fizzbuzz.no"),
        Name(Some("boo"), None, Some("baa"))
      )
      saveAndValidate(usr, Created)

      val res = service.findByUsername(usr.username)
      res must_!= None
      res.get.name must_== usr.name
      res.get.email must_== usr.email
      res.get.username must_== usr.username
    }

    "be possible to update a User" in {
      val usr = buildUser(
        Username("liiloo"),
        Email("liiloo@fizzbuzz.no"),
        Name(Some("lii"), None, Some("loo"))
      )
      saveAndValidate(usr, Created)

      val res1 = service.findById(usr.id.get)
      res1 must_!= None

      val mod =
        res1.get.copy(name = res1.get.name.map(_.copy(middle = Some("laa"))))
      saveAndValidate(mod, Updated)

      val res2 = service.findById(usr.id.get)
      res2 must_!= None
      res2.get.name must_== mod.name
      res2.get.email must_== usr.email
      res2.get.username must_== usr.username
    }
  }

}

trait WithUserService { self: MongoSpec =>
  lazy val service = new UserService(
    new MongoDBUserRepository(self.configuration)
  )
}
