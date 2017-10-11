package services.party

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.base._
import models.party.User
import net.scalytica.symbiotic.core.DocManagementService
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import repository.UserRepository

import scala.concurrent.ExecutionContext.Implicits.global

trait UserServiceSpec
    extends WordSpecLike
    with MustMatchers
    with ScalaFutures
    with OptionValues {

  def repo: UserRepository
  def docService: DocManagementService

  lazy val service = new UserService()(global, repo, docService)

  def buildUser(uname: Username, email: Email, name: Name): User =
    User(
      id = SymbioticUserId.createOpt(),
      loginInfo = LoginInfo(CredentialsProvider.ID, uname.value),
      v = None,
      username = uname,
      email = email,
      name = Some(name),
      dateOfBirth = Some(DateTime.now().minusYears(20)), // scalastyle:ignore
      gender = Some(Male)
    )

  def saveUser(usr: User): Either[String, SymbioticUserId] = {
    service.save(usr).futureValue
  }

  "When using the UserService it" should {
    "be possible to add a new User" in {
      val usr = buildUser(
        Username("foobar"),
        Email("foobar@fizzbuzz.no"),
        Name(Some("foo"), None, Some("bar"))
      )
      saveUser(usr) mustBe a[Right[_, _]]
    }

    "be possible to find a User by UserId" in {
      val usr = buildUser(
        Username("fiifaa"),
        Email("fiifaa@fizzbuzz.no"),
        Name(Some("fii"), None, Some("faa"))
      )

      val saved = saveUser(usr)
      saved mustBe a[Right[_, _]]
      val uid = saved.right.toOption.value

      val res = service.findById(uid).futureValue
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

      val saved = saveUser(usr)
      saved mustBe a[Right[_, _]]
      val uid = saved.right.toOption

      val res = service.findByUsername(usr.username).futureValue
      res must not be empty
      res.get.id mustBe uid
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

      val saved = saveUser(usr)
      saved mustBe a[Right[_, _]]
      val uid = saved.right.toOption

      val res1 = service.findById(uid.get).futureValue
      res1 must not be empty

      val mod =
        res1.get.copy(name = res1.get.name.map(_.copy(middle = Some("laa"))))

      saveUser(mod)

      val res2 = service.findById(uid.get).futureValue
      res2 must not be empty
      res2.get.id mustBe uid
      res2.get.name mustBe mod.name
      res2.get.email mustBe usr.email
      res2.get.username mustBe usr.username
    }
  }

}
