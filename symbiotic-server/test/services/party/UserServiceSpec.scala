package services.party

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.base.Gender.Male
import models.base.{Email, Name, SymbioticUserId, Username}
import models.party.User
import net.scalytica.symbiotic.api.types.{Created, Success, Updated}
import net.scalytica.symbiotic.test.MongoSpec
import org.joda.time.DateTime
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.mutable.ExecutionEnvironment
import repository.mongodb.party.MongoDBUserRepository

import scala.concurrent.Await
import scala.concurrent.duration._

class UserServiceSpec
    extends Specification
    with ExecutionEnvironment
    with MongoSpec {

  lazy val repo    = new MongoDBUserRepository(configuration)
  lazy val service = new UserService(repo)

  val timeout: Duration = 2 seconds

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

  def saveAndValidate[A <: Success](
      usr: User,
      s: A
  )(implicit ee: ExecutionEnv) =
    Await.result(service.save(usr), timeout) must_== s

  // scalastyle:off method.length
  def is(implicit ee: ExecutionEnv) = {

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

        val res = Await.result(service.findById(usr.id.get), timeout)
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

        val res = Await.result(service.findByUsername(usr.username), timeout)
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

        val res1 = Await.result(service.findById(usr.id.get), timeout)
        res1 must_!= None

        val mod =
          res1.get.copy(name = res1.get.name.map(_.copy(middle = Some("laa"))))
        saveAndValidate(mod, Updated)

        val res2 = Await.result(service.findById(usr.id.get), timeout)
        res2 must_!= None
        res2.get.name must_== mod.name
        res2.get.email must_== usr.email
        res2.get.username must_== usr.username
      }
    }
  }
  // scalastyle:on method.length

}
