package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.persistence.FolderRepository
import net.scalytica.symbiotic.api.types.CommandStatusTypes._
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MongoDBFolderRepository(
    val configuration: Config
) extends FolderRepository
    with MongoFSRepository {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def get(folderId: FolderId)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[Folder]] = Future {
    collection
      .findOne(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          FidKey.full      -> folderId.value,
          IsFolderKey.full -> true
        )
      )
      .map(folder_fromBSON)
  }

  override def exists(at: Path)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Boolean] = Future {
    collection
      .findOne(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          PathKey.full     -> at.materialize,
          IsFolderKey.full -> true
        )
      )
      .isDefined
  }

  private[this] def doesExist(p: Path)(implicit uid: UserId): Boolean = {
    collection
      .findOne(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          PathKey.full     -> p.materialize,
          IsFolderKey.full -> true
        )
      )
      .isDefined
  }

  override def filterMissing(p: Path)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[List[Path]] = Future {

    case class CurrPathMiss(path: String, missing: List[Path])

    val segments = p.path.split("/").filterNot(_.isEmpty)

    // Left fold over the path segments and identify the ones that doesn't exist
    segments
      .foldLeft[CurrPathMiss](CurrPathMiss("", List.empty)) {
        case (prev: CurrPathMiss, seg: String) =>
          val p    = if (prev.path.isEmpty) seg else s"${prev.path}/$seg"
          val next = Path(p)
          if (doesExist(next)) CurrPathMiss(p, prev.missing)
          else CurrPathMiss(p, next +: prev.missing)
      }
      .missing
  }

  override def save(f: Folder)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]] = exists(f).map { folderExists =>
    if (!folderExists) {
      val id  = f.id.getOrElse(UUID.randomUUID())
      val fid = Some(f.metadata.fid.getOrElse(FileId.create()))
      val sd = MongoDBObject(
        "_id"       -> id.toString,
        "filename"  -> f.filename,
        MetadataKey -> managedfmd_toBSON(f.metadata.copy(fid = fid))
      )
      Try {
        logger.debug(s"Creating folder $f")
        collection.save(sd)
        fid
      }.recover {
        case e: Throwable =>
          logger.error(s"An error occurred trying to save $f", e)
          None
      }.toOption.flatten
    } else {
      None
    }
  }

  override def move(orig: Path, mod: Path)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[CommandStatus[Int]] = Future {
    val qry = MongoDBObject(
      OwnerKey.full -> uid.value,
      PathKey.full  -> orig.materialize
    )
    val upd =
      $set("filename" -> mod.nameOfLast, PathKey.full -> mod.materialize)

    Try {
      val res = collection.update(qry, upd)
      if (res.getN > 0) CommandOk(res.getN)
      else CommandKo(0)
    }.recover {
      case e: Throwable =>
        CommandError(0, Option(e.getMessage))
    }.get
  }

}
