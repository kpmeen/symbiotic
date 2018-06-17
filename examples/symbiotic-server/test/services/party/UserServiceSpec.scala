package services.party

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.base._
import models.party.User
import net.scalytica.symbiotic.time.SymbioticDateTime._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}

trait UserServiceSpec
    extends WordSpecLike
    with MustMatchers
    with ScalaFutures
    with OptionValues {

  def service: UserService

  def buildUser(uname: Username, email: Email, name: Name): User =
    User(
      id = SymbioticUserId.createOpt(),
      loginInfo = LoginInfo(CredentialsProvider.ID, uname.value),
      v = None,
      username = uname,
      email = email,
      name = Some(name),
      dateOfBirth = Some(now.minusYears(20)), // scalastyle:ignore
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
      res.value.name mustBe usr.name
      res.value.email mustBe usr.email
      res.value.username mustBe usr.username
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
      res.value.id mustBe uid
      res.value.name mustBe usr.name
      res.value.email mustBe usr.email
      res.value.username mustBe usr.username
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

      val r1 = service.findById(uid.value).futureValue
      r1 must not be empty

      val mod =
        r1.value.copy(name = r1.value.name.map(_.copy(middle = Some("laa"))))

      saveUser(mod)

      val r2 = service.findById(uid.value).futureValue
      r2 must not be empty
      r2.value.id mustBe uid
      r2.value.name mustBe mod.name
      r2.value.email mustBe usr.email
      r2.value.username mustBe usr.username
    }
  }

}
