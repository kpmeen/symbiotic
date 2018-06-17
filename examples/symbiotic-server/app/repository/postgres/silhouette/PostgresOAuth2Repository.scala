package repository.postgres.silhouette

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import net.scalytica.symbiotic.postgres.SymbioticDb
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import repository.OAuth2Repository
import repository.postgres.ExtraColumnMappers

import scala.concurrent.{ExecutionContext, Future}

class PostgresOAuth2Repository @Inject()(
    configuration: Configuration,
    ec: ExecutionContext
) extends OAuth2Repository
    with SymbioticDb
    with ExtraColumnMappers {

  import profile.api._

  private[this] val log = Logger(getClass)

  private[this] implicit val exec = ec

  override lazy val config = configuration.underlying

  private[this] val oauthTable = TableQuery[Oauth2InfoTable]

  private[this] def findQuery(li: LoginInfo) = {
    oauthTable.filter { r =>
      r.providerId === li.providerID &&
      r.providerKey === li.providerKey
    }
  }

  override def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
    db.run(findQuery(loginInfo).result.headOption).map(_.map(_.asOAuth2Info))
  }

  override def add(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = {
    val oar    = OAuth2InfoRow.init(loginInfo, authInfo)
    val action = oauthTable returning oauthTable.map(_.id) += oar

    db.run(action.transactionally).map(_ => authInfo)
  }

  override def update(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = {
    val updAction = findQuery(loginInfo).map { r =>
      (r.accessToken, r.tokenType, r.expiresIn, r.refreshToken, r.params)
    }.update(
      (
        authInfo.accessToken,
        authInfo.tokenType,
        authInfo.expiresIn,
        authInfo.refreshToken,
        authInfo.params.map(Json.toJson[Map[String, String]])
      )
    )

    db.run(updAction.transactionally).flatMap { num =>
      if (0 < num)
        Future.successful(authInfo)
      else
        find(loginInfo).map(_.getOrElse {
          throw new IllegalStateException("Expected to find a OAuth2Info.")
        })
    }
  }

  override def save(
      loginInfo: LoginInfo,
      authInfo: OAuth2Info
  ): Future[OAuth2Info] = {
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

  case class OAuth2InfoRow(
      id: Option[Int],
      providerId: String,
      providerKey: String,
      accessToken: String,
      tokenType: Option[String],
      expiresIn: Option[Int],
      refreshToken: Option[String],
      params: Option[JsValue]
  ) {

    def asOAuth2Info: OAuth2Info = {
      OAuth2Info(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        params = params.map(_.as[Map[String, String]])
      )
    }

  }

  object OAuth2InfoRow {
    def init(li: LoginInfo, oa: OAuth2Info): OAuth2InfoRow = {
      OAuth2InfoRow(
        id = None,
        providerId = li.providerID,
        providerKey = li.providerKey,
        accessToken = oa.accessToken,
        tokenType = oa.tokenType,
        expiresIn = oa.expiresIn,
        refreshToken = oa.refreshToken,
        params = oa.params.map(Json.toJson[Map[String, String]])
      )
    }
  }

  class Oauth2InfoTable(
      val tag: Tag
  ) extends Table[OAuth2InfoRow](tag, Some(dbSchema), "oauth2info") {

    val id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    val providerId   = column[String]("provider_id")
    val providerKey  = column[String]("provider_key")
    val accessToken  = column[String]("access_token")
    val tokenType    = column[Option[String]]("token_type")
    val expiresIn    = column[Option[Int]]("expires_in")
    val refreshToken = column[Option[String]]("refresh_token")
    val params       = column[Option[JsValue]]("params")

    // scalastyle:off method.name
    override def * =
      (
        id.?,
        providerId,
        providerKey,
        accessToken,
        tokenType,
        expiresIn,
        refreshToken,
        params
      ) <> ((OAuth2InfoRow.apply _).tupled, OAuth2InfoRow.unapply _)

    // scalastyle:on method.name

  }

}
