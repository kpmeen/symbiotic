package repository.postgres.party

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import models.base._
import models.party.User
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.{
  UserStamp,
  VersionStamp
}
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repository.UserRepository
import repository.postgres.ExtraColumnMappers

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PostgresUserRepository @Inject()(configuration: Configuration)
    extends UserRepository
    with SymbioticDb
    with ExtraColumnMappers {

  // This _must_ be lazy, otherwise it will trigger an
  // NPE in the SymbioticDb trait
  override lazy val config = configuration.underlying

  import profile.api._

  private[this] val log = Logger(getClass)

  private[this] val usersTable = TableQuery[UsersTable]

  private[this] def insertAction(row: UserRow): DBIO[SymbioticUserId] =
    usersTable returning usersTable.map(_.id) += row

  private[this] def findByIdQuery(id: SymbioticUserId) =
    usersTable.filter(_.id === id)

  private[this] def findByUsernameQuery(uname: Username) =
    usersTable.filter(_.username === uname)

  private[this] def findByLoginInfoQuery(li: LoginInfo) =
    usersTable.filter { u =>
      u.providerId === li.providerID &&
      u.providerKey === li.providerKey
    }

  override def save(
      user: User
  )(implicit ec: ExecutionContext): Future[Either[String, SymbioticUserId]] = {
    val row = userToRow(user)

    val action = user.id.map { id =>
      for {
        exists <- findByIdQuery(id).exists.result
        res <- {
          if (exists) findByIdQuery(id).update(row).map(_ => Right(id))
          else
            insertAction(row).map { id =>
              log.debug(s"Inserted User with id $id")
              Right(id)
            }
        }
      } yield res
    }.getOrElse {
      log.debug(s"User does not exist. Performing insert.")
      insertAction(row).map { id =>
        log.debug(s"Inserted User with id $id")
        Right(id)
      }
    }

    db.run(action.transactionally).recover {
      case NonFatal(ex) =>
        val msg = "An error occurred trying to save a User"
        log.error(msg, ex)
        Left(msg)
    }
  }

  override def findById(
      id: UserId
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    val query = findByIdQuery(id).result.headOption

    db.run(query)
      .map { res =>
        log.debug(s"findById($id) = $res")
        res.map(userFromRow)
      }
      .recover {
        case NonFatal(ex) =>
          log.error(s"An error occurred trying to fetch User $id", ex)
          None
      }
  }

  override def findByUsername(
      username: Username
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    val query = findByUsernameQuery(username).result.headOption

    db.run(query).map(_.map(userFromRow)).recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to fetch User $username", ex)
        None
    }
  }

  override def findByLoginInfo(
      loginInfo: LoginInfo
  )(implicit ec: ExecutionContext): Future[Option[User]] = {
    val query = findByLoginInfoQuery(loginInfo).result.headOption

    db.run(query).map(_.map(userFromRow)).recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to fetch User by login info", ex)
        None
    }
  }

  case class UserRow(
      id: Option[SymbioticUserId],
      providerId: String,
      providerKey: String,
      version: Option[Int],
      createdBy: Option[SymbioticUserId],
      createdDate: Option[DateTime],
      modifiedBy: Option[SymbioticUserId],
      modifiedDate: Option[DateTime],
      username: Username,
      email: Email,
      firstName: Option[String],
      middleName: Option[String],
      lastName: Option[String],
      dateOfBirth: Option[DateTime],
      gender: Option[Gender],
      active: Boolean,
      avatarUrl: Option[String],
      useSocialAvatar: Boolean
  )

  def userToRow(u: User): UserRow = {
    UserRow(
      id = u.id,
      providerId = u.loginInfo.providerID,
      providerKey = u.loginInfo.providerKey,
      version = u.v.map(_.version),
      createdBy = u.v.flatMap(_.created.map(_.by)),
      createdDate = u.v.flatMap(_.created.map(_.date)),
      modifiedBy = u.v.flatMap(_.modified.map(_.by)),
      modifiedDate = u.v.flatMap(_.modified.map(_.date)),
      username = u.username,
      email = u.email,
      firstName = u.name.flatMap(_.first),
      middleName = u.name.flatMap(_.middle),
      lastName = u.name.flatMap(_.last),
      dateOfBirth = u.dateOfBirth,
      gender = u.gender,
      active = u.active,
      avatarUrl = u.avatarUrl,
      useSocialAvatar = u.useSocialAvatar
    )
  }

  def userFromRow(ur: UserRow): User = {
    User(
      id = ur.id,
      loginInfo = LoginInfo(
        providerID = ur.providerId,
        providerKey = ur.providerKey
      ),
      v = ur.version.map { v =>
        VersionStamp(
          version = v,
          created = for {
            by   <- ur.createdBy
            date <- ur.createdDate
          } yield UserStamp(date, by),
          modified = for {
            by   <- ur.modifiedBy
            date <- ur.modifiedDate
          } yield UserStamp(date, by)
        )
      },
      username = ur.username,
      email = ur.email,
      name = Name.emptyAsOption(ur.firstName, ur.middleName, ur.lastName),
      dateOfBirth = ur.dateOfBirth,
      gender = ur.gender,
      active = ur.active,
      avatarUrl = ur.avatarUrl,
      useSocialAvatar = ur.useSocialAvatar
    )
  }

  class UsersTable(
      val tag: Tag
  ) extends Table[UserRow](tag, Some(dbSchema), "users") {
    // scalastyle:off line.size.limit
    val id               = column[SymbioticUserId]("id", O.PrimaryKey, O.AutoInc)
    val providerId       = column[String]("provider_id")
    val providerKey      = column[String]("provider_key")
    val version          = column[Option[Int]]("version")
    val createdBy        = column[Option[SymbioticUserId]]("created_by")
    val createdDate      = column[Option[DateTime]]("created_date")
    val modifiedBy       = column[Option[SymbioticUserId]]("modified_by")
    val modifiedDate     = column[Option[DateTime]]("modified_date")
    val username         = column[Username]("username")
    val email            = column[Email]("email")
    val firstName        = column[Option[String]]("first_name")
    val middleName       = column[Option[String]]("middle_name")
    val lastName         = column[Option[String]]("last_name")
    val dateOfBirth      = column[Option[DateTime]]("date_of_birth")
    val gender           = column[Option[Gender]]("gender")
    val active           = column[Boolean]("active")
    val avatarUrl        = column[Option[String]]("avatar_url")
    val userSocialAvatar = column[Boolean]("use_social_avatar")
    // scalastyle:on line.size.limit

    // scalastyle:off method.name
    override def * =
      (
        id.?,
        providerId,
        providerKey,
        version,
        createdBy,
        createdDate,
        modifiedBy,
        modifiedDate,
        username,
        email,
        firstName,
        middleName,
        lastName,
        dateOfBirth,
        gender,
        active,
        avatarUrl,
        userSocialAvatar
      ) <> ((UserRow.apply _).tupled, UserRow.unapply _)

    // scalastyle:on method.name

  }

}
