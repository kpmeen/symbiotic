package repository.postgres.silhouette

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import net.scalytica.symbiotic.postgres.SymbioticDb
import play.api.{Configuration, Logger}
import repository.PasswordAuthRepository
import repository.postgres.ExtraColumnMappers

import scala.concurrent.{ExecutionContext, Future}

class PostgresPasswordAuthRepository @Inject()(
    configuration: Configuration,
    ec: ExecutionContext
) extends PasswordAuthRepository
    with SymbioticDb
    with ExtraColumnMappers {

  private implicit val exec = ec

  import profile.api._

  private val log = Logger(getClass)

  override lazy val config = configuration.underlying

  val passwordInfoTable = TableQuery[PasswordInfoTable]

  private[this] def findQuery(loginInfo: LoginInfo) = {
    passwordInfoTable.filter { p =>
      p.providerId === loginInfo.providerID &&
      p.providerKey === loginInfo.providerKey
    }
  }

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    db.run(findQuery(loginInfo).result.headOption).map(_.map(_.asPasswordInfo))
  }

  override def add(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = {
    val pir    = PasswordInfoRow.init(loginInfo, authInfo)
    val action = passwordInfoTable returning passwordInfoTable.map(_.id) += pir

    db.run(action.transactionally).map(_ => authInfo)
  }

  override def update(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = {
    val updAction = findQuery(loginInfo)
      .map(row => (row.hasher, row.password, row.salt))
      .update((authInfo.hasher, authInfo.password, authInfo.salt))

    db.run(updAction.transactionally).flatMap { num =>
      if (0 < num)
        Future.successful(authInfo)
      else
        find(loginInfo).map(_.getOrElse {
          throw new IllegalStateException("Expected to find a PasswordInfo.")
        })
    }
  }

  override def save(
      loginInfo: LoginInfo,
      authInfo: PasswordInfo
  ): Future[PasswordInfo] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None    => add(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    db.run(findQuery(loginInfo).delete.transactionally).map { res =>
      if (0 < res) log.debug(s"No rows matching $loginInfo were removed")
      else log.debug(s"Removed $res rows matching $loginInfo")
    }
  }

  case class PasswordInfoRow(
      id: Option[Int],
      providerId: String,
      providerKey: String,
      hasher: String,
      password: String,
      salt: Option[String]
  ) {

    def asPasswordInfo = PasswordInfo(hasher, password, salt)

  }

  object PasswordInfoRow {
    def init(li: LoginInfo, ai: PasswordInfo): PasswordInfoRow = {
      PasswordInfoRow(
        None,
        li.providerID,
        li.providerKey,
        ai.hasher,
        ai.password,
        ai.salt
      )
    }
  }

  class PasswordInfoTable(val tag: Tag)
      extends Table[PasswordInfoRow](tag, Some(dbSchema), "password_info") {

    val id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    val providerId  = column[String]("provider_id")
    val providerKey = column[String]("provider_key")
    val hasher      = column[String]("hasher")
    val password    = column[String]("password")
    val salt        = column[Option[String]]("salt")

    // scalastyle:off method.name
    def * =
      (
        id.?,
        providerId,
        providerKey,
        hasher,
        password,
        salt
      ) <> ((PasswordInfoRow.apply _).tupled, PasswordInfoRow.unapply _)

    // scalastyle:on method.name

  }

}
