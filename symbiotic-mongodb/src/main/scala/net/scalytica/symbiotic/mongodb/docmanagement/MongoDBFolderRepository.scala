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
import scala.util.control.NonFatal

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

  override def get(at: Path)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[Folder]] = Future {
    collection
      .findOne(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          PathKey.full     -> at.materialize,
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

    // Left fold over the path segments and identify the ones that don't exist
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

  private def saveFolder(f: Folder)(
      implicit uid: UserId,
      tu: TransUserId
  ): Option[FileId] = {
    val fid: Option[FileId] = Some(f.metadata.fid.getOrElse(FileId.create()))
    val id: UUID            = f.id.getOrElse(UUID.randomUUID())
    val mdBson: DBObject    = f.metadata.copy(fid = fid)
    val ctype = f.fileType
      .map(t => MongoDBObject("contentType" -> t))
      .getOrElse(MongoDBObject.empty)
    val dbo = MongoDBObject(
      "_id"       -> id.toString,
      "filename"  -> f.filename,
      MetadataKey -> mdBson
    ) ++ ctype

    Try {
      logger.debug(s"Creating folder")
      collection.save(dbo)
      fid
    }.recover {
      case NonFatal(e) =>
        logger.error(s"An error occurred saving a Folder: $f", e)
        None
    }.toOption.flatten
  }

  private def updateFolder(f: Folder)(
      implicit uid: UserId,
      tu: TransUserId
  ): Option[FileId] = {
    f.metadata.fid.map { fileId =>
      val set   = Seq.newBuilder[(String, Any)]
      val unset = Seq.newBuilder[String]

      set += VersionKey.full -> f.metadata.version
      f.fileType
        .fold[Unit](unset += "contentType")(ft => set += "contentType" -> ft)
      f.metadata.description.fold[Unit](unset += DescriptionKey.full)(
        d => set += DescriptionKey.full -> d
      )
      f.metadata.extraAttributes.fold[Unit](unset += ExtraAttributesKey.full)(
        ea => set += ExtraAttributesKey.full -> extraAttribs_toBSON(ea)
      )

      collection.update(
        MongoDBObject(
          OwnerKey.full    -> uid.value,
          FidKey.full      -> fileId.value,
          IsFolderKey.full -> true
        ),
        $set(set.result(): _*) ++ $unset(unset.result: _*)
      )
      fileId
    }
  }

  // scalastyle:off
  override def save(f: Folder)(
      implicit uid: UserId,
      tu: TransUserId,
      ec: ExecutionContext
  ): Future[Option[FileId]] = exists(f).map { folderExists =>
    if (!folderExists) saveFolder(f)
    else updateFolder(f)
  }
  // scalastyle:on

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
      case NonFatal(e) =>
        CommandError(0, Option(e.getMessage))
    }.get
  }

}
