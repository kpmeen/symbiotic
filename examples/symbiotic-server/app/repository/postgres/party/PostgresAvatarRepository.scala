package repository.postgres.party

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import models.base.SymbioticUserId
import models.party.{Avatar, AvatarMetadata}
import net.scalytica.symbiotic.api.types.File
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.fs.FileSystemIO
import net.scalytica.symbiotic.postgres.SymbioticDb
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repository.AvatarRepository
import repository.postgres.ExtraColumnMappers

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class PostgresAvatarRepository @Inject()(
    implicit
    configuration: Configuration,
    sys: ActorSystem,
    mat: Materializer
) extends AvatarRepository
    with SymbioticDb
    with ExtraColumnMappers {

  // This _must_ be lazy, otherwise it will trigger an
  // NPE in the SymbioticDb trait
  override lazy val config = configuration.underlying

  import profile.api._

  private val log = Logger(getClass)

  private lazy val fs = new FileSystemIO(config)

  private val avatarTable = TableQuery[AvatarTable]

  private def findForUserQuery(suid: SymbioticUserId) = {
    avatarTable.filter(_.userId === suid).result.headOption
  }

  private def oldAvatarsAction(
      uid: SymbioticUserId,
      except: Option[UUID] = None
  )(implicit ec: ExecutionContext): DBIO[Seq[Avatar]] = {
    val base = avatarTable.filter(_.filename === uid.value)

    val query = except.map(id => base.filter(_.id =!= id)).getOrElse(base)

    query.result.map(_.map(avatarFromRow))
  }

  private def insertRowAction(row: AvatarRow): DBIO[UUID] = {
    avatarTable returning avatarTable.map(_.id) += row
  }

  private def removeOldAction(
      uid: SymbioticUserId,
      ids: Seq[UUID]
  ): DBIO[Int] =
    avatarTable.filter { a =>
      (a.id inSet ids) &&
      a.userId === uid
    }.delete

  override def save(
      a: Avatar
  )(implicit ec: ExecutionContext): Future[Option[UUID]] = {
    val add = a.copy(createdDate = Option(DateTime.now))
    val row = avatarToRow(add)

    val action = insertRowAction(row)

    db.run(action.transactionally)
      .flatMap { uuid =>
        val avatarFile: File = add.copy(id = Some(uuid))
        fs.write(avatarFile).map {
          case Right(()) =>
            log.debug(
              s"Saved avatar file for ${add.metadata.uid} with UUID $uuid"
            )
            // Try to remove old avatar images.
            removeOld(add.metadata.uid, uuid)
            Some(uuid)

          case Left(err) =>
            log.warn(err)
            None
        }

      }
      .recover {
        case NonFatal(ex) =>
          log
            .error(s"An error occurred saving avatar for ${a.metadata.uid}", ex)
          None
      }
  }

  override def get(
      uid: UserId
  )(implicit ec: ExecutionContext): Future[Option[Avatar]] = {
    val query = findForUserQuery(uid)

    db.run(query).map { res =>
      res.map { row =>
        val avatar = avatarFromRow(row)
        avatar.copy(stream = fs.read(avatar))
      }
    }
  }

  private[this] def removeOld(
      uid: UserId,
      except: UUID
  )(implicit ec: ExecutionContext): Future[Unit] = {
    val action = for {
      old <- oldAvatarsAction(uid, Some(except))
      rem <- removeOldAction(uid, old.flatMap(_.id))
    } yield (old, rem)

    db.run(action.transactionally).map {
      case (old, rem) =>
        old.foreach { oldAvatar =>
          fs.eraseFile(oldAvatar)
        }
        log.debug(s"Removed ${old.size} files and $rem metadata entries.")
    }
  }

  override def remove(
      uid: UserId
  )(implicit ec: ExecutionContext): Future[Unit] = {
    val action = for {
      old <- oldAvatarsAction(uid)
      rem <- removeOldAction(uid, old.flatMap(_.id))
    } yield (old, rem)

    db.run(action.transactionally).map {
      case (old, rem) =>
        old.foreach { oldAvatar =>
          fs.eraseFile(oldAvatar)
        }
        log.debug(s"Removed ${old.size} files and $rem metadata entries.")
    }
  }

  override def remove(
      uid: UserId,
      ids: Seq[UUID]
  )(implicit ec: ExecutionContext): Future[Unit] = Future.successful {
    log.warn(
      "Method PostgresAvatarRepository#remove(UserId, Seq[UUID]) is not " +
        "implemented. Use PostgresAvatarRepository#remove(UserId) instead."
    )
  }

  type AvatarRow =
    (
        Option[UUID], // id
        SymbioticUserId, // user_id
        String, // filename
        Option[String], // file_type
        Option[String], // length
        DateTime // created_date
    )

  def avatarToRow(a: Avatar): AvatarRow = {
    (
      a.id,
      a.metadata.uid,
      a.filename,
      a.fileType,
      a.length,
      a.createdDate.getOrElse(DateTime.now)
    )
  }

  def avatarFromRow(r: AvatarRow) = {
    Avatar(
      id = r._1,
      metadata = AvatarMetadata(r._2),
      filename = r._3,
      fileType = r._4,
      length = r._5,
      createdDate = Option(r._6)
    )
  }

  class AvatarTable(val tag: Tag)
      extends Table[AvatarRow](tag, Some(dbSchema), "avatars") {

    val id          = column[UUID]("id", O.PrimaryKey, O.AutoInc)
    val userId      = column[SymbioticUserId]("user_id")
    val filename    = column[String]("filename")
    val fileType    = column[Option[String]]("file_type")
    val length      = column[Option[String]]("length")
    val createdDate = column[DateTime]("created_date")

    // scalastyle:off method.name
    override def * = (
      id.?,
      userId,
      filename,
      fileType,
      length,
      createdDate
    )

  }

}
