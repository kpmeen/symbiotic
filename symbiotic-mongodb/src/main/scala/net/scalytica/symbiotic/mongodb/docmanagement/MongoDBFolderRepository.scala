package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.FolderRepository
import net.scalytica.symbiotic.api.types.CommandStatusTypes._
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes.{
  LockApplied,
  LockError,
  LockOpStatus,
  LockRemoved
}
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class MongoDBFolderRepository(
    val configuration: Config
) extends FolderRepository
    with MongoFSRepository {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /*
    TODO: The current implementation is rather naive and just calls `get(fid)`.
    This won't be enough once folders support versioning.
   */
  override def findLatestBy(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = get(fid)

  override def get(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = Future {
    collection
      .findOne(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          FidKey.full $eq folderId.value,
          IsFolderKey.full $eq true,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        )
      )
      .map(folder_fromBSON)
  }

  override def get(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[Folder]] = Future {
    collection
      .findOne(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          PathKey.full $eq at.materialize,
          IsFolderKey.full $eq true,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        )
      )
      .map(folder_fromBSON)
  }

  override def exists(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = Future {
    collection
      .findOne(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          PathKey.full $eq at.materialize,
          IsFolderKey.full $eq true,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        )
      )
      .isDefined
  }

  private[this] def doesExist(
      p: Path
  )(implicit ctx: SymbioticContext): Boolean = {
    collection
      .findOne(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          PathKey.full $eq p.materialize,
          IsFolderKey.full $eq true,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        )
      )
      .isDefined
  }

  override def filterMissing(p: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[List[Path]] = Future {

    case class CurrPathMiss(path: String, missing: List[Path])

    val segments = p.value.split("/").filterNot(_.isEmpty)

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
      implicit ctx: SymbioticContext
  ): Option[FileId] = {
    val fid: Option[FileId] = Some(f.metadata.fid.getOrElse(FileId.create()))
    val id: UUID            = f.id.getOrElse(UUID.randomUUID())
    val mdBson: DBObject    = f.metadata.copy(fid = fid)
    val ctype = f.fileType
      .map(t => MongoDBObject("contentType" -> t))
      .getOrElse(MongoDBObject.empty)
    val dbo = MongoDBObject(
      "_id"        -> id.toString,
      "filename"   -> f.filename,
      "uploadDate" -> f.createdDate.getOrElse(DateTime.now().toDate),
      MetadataKey  -> mdBson
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
      implicit ctx: SymbioticContext
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
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          FidKey.full $eq fileId.value,
          IsFolderKey.full $eq true,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        ),
        $set(set.result(): _*) ++ $unset(unset.result: _*)
      )
      fileId
    }
  }

  override def save(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = exists(f).map { folderExists =>
    if (!folderExists) saveFolder(f)
    else updateFolder(f)
  }

  override def move(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[CommandStatus[Int]] = Future {
    val qry = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      PathKey.full $eq orig.materialize,
      IsFolderKey.full $eq true,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
    )
    val upd =
      $set("filename" -> mod.nameOfLast, PathKey.full -> mod.materialize)

    Try {
      val res = collection.update(qry, upd)
      if (res.getN > 0) CommandOk(res.getN)
      else CommandKo(0)
    }.recover {
      case NonFatal(e) => CommandError(0, Option(e.getMessage))
    }.get
  }

  override def lock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] = {
    lockManagedFile(fid) {
      case (dbId, lock) =>
        Future {
          val qry = $and(
            FidKey.full $eq fid.value,
            OwnerIdKey.full $eq ctx.owner.id.value,
            IsFolderKey.full $eq true,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
          )
          val upd = $set(LockKey.full -> lock_toBSON(lock))
          if (collection.update(qry, upd).getN > 0) {
            LockApplied(Option(lock))
          } else {
            val msg = "Locking query did not match any documents"
            logger.warn(msg)
            LockError(msg)
          }
        }
    }
  }

  override def unlock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] =
    unlockManagedFile(fid) { dbId =>
      Future {
        val res = collection.update(
          $and(
            "_id" $eq dbId.toString,
            OwnerIdKey.full $eq ctx.owner.id.value,
            IsFolderKey.full $eq true,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
          ),
          $unset(LockKey.full)
        )
        if (res.getN > 0) LockRemoved(s"Successfully unlocked $fid")
        else LockError("Unlocking query did not match any documents")
      }
    }

  override def editable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = Future {
    val qry = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      IsFolderKey.full $eq true,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      $or(from.allPaths.map { p =>
        MongoDBObject(PathKey.full -> p.materialize)
      })
    )
    collection.find(qry).map(folder_fromBSON).forall(_.metadata.lock.isEmpty)
  }

}
