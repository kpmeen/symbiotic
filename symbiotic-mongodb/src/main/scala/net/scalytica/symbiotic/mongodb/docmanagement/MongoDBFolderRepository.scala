package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import com.mongodb.casbah.Imports._
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.repository.FolderRepository
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

  private val log = LoggerFactory.getLogger(this.getClass)

  override def findLatestBy(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] = get(fid)

  override def get(folderId: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] =
    Future {
      collection
        .findOne(
          $and(
            OwnerIdKey.full $eq ctx.owner.id.value,
            FidKey.full $eq folderId.value,
            IsFolderKey.full $eq true,
            IsDeletedKey.full $eq false,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
          )
        )
        .map(mdb => Ok(folderFromBSON(mdb)))
        .getOrElse(NotFound())
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to get folder $folderId.", ex)
        Failed(ex.getMessage)
    }

  override def get(at: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Folder]] =
    Future {
      collection
        .findOne(
          $and(
            OwnerIdKey.full $eq ctx.owner.id.value,
            PathKey.full $eq at.materialize,
            IsFolderKey.full $eq true,
            IsDeletedKey.full $eq false,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
          )
        )
        .map(mdb => Ok(folderFromBSON(mdb)))
        .getOrElse(NotFound())
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to get folder $at.", ex)
        Failed(ex.getMessage)
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
          IsDeletedKey.full $eq false,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        )
      )
      .isDefined
  }

  private[this] def doesExist(p: Path)(
      implicit ctx: SymbioticContext
  ): Boolean = {
    collection
      .findOne(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          PathKey.full $eq p.materialize,
          IsFolderKey.full $eq true,
          IsDeletedKey.full $eq false,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        )
      )
      .isDefined
  }

  override def filterMissing(p: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[List[Path]]] =
    Future {

      case class CurrPathMiss(path: String, missing: List[Path])

      val segments = p.value.split("/").filterNot(_.isEmpty)

      // Left fold over the path segments and identify the ones that don't exist
      Ok(
        segments
          .foldLeft[CurrPathMiss](CurrPathMiss("", List.empty)) {
            case (prev: CurrPathMiss, seg: String) =>
              val p    = if (prev.path.isEmpty) seg else s"${prev.path}/$seg"
              val next = Path(p)
              if (doesExist(next)) CurrPathMiss(p, prev.missing)
              else CurrPathMiss(p, next +: prev.missing)
          }
          .missing
      )
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying to get missing folders in $p.", ex)
        Failed(ex.getMessage)
    }

  private def saveFolder(f: Folder): SaveResult[FileId] = {
    val fid: FileId      = f.metadata.fid.getOrElse(FileId.create())
    val id: UUID         = f.id.getOrElse(UUID.randomUUID())
    val mdBson: DBObject = f.metadata.copy(fid = Some(fid))
    val ctype = f.fileType
      .map(t => MongoDBObject("contentType" -> t))
      .getOrElse(MongoDBObject.empty)
    val dbo = MongoDBObject(
      "_id"        -> id.toString,
      "filename"   -> f.filename,
      "uploadDate" -> f.createdDate.getOrElse(DateTime.now()).toDate,
      MetadataKey  -> mdBson
    ) ++ ctype

    collection.save(dbo)
    Ok(fid) // Safe since we're creating it if missing above
  }

  private def updateFolder(f: Folder)(
      implicit ctx: SymbioticContext
  ): SaveResult[FileId] = {
    f.metadata.fid.map { fileId =>
      val set   = Seq.newBuilder[(String, Any)]
      val unset = Seq.newBuilder[String]

      set += VersionKey.full -> f.metadata.version
      f.fileType.fold[Unit](unset += "contentType")(
        ft => set += "contentType" -> ft
      )
      f.metadata.description.fold[Unit](unset += DescriptionKey.full)(
        d => set += DescriptionKey.full -> d
      )
      f.metadata.extraAttributes.fold[Unit](unset += ExtraAttributesKey.full)(
        ea => set += ExtraAttributesKey.full -> extraAttribsToBSON(ea)
      )

      val res = collection.update(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          FidKey.full $eq fileId.value,
          IsFolderKey.full $eq true,
          IsDeletedKey.full $eq false,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
        ),
        $set(set.result: _*) ++ $unset(unset.result: _*)
      )

      if (res.getN == 1) Ok(fileId) else NotModified()
    }.getOrElse {
      InvalidData(s"Can't update folder because it's missing FolderId")
    }
  }

  override def save(f: Folder)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FileId]] =
    exists(f).map {
      case fe: Boolean if !fe =>
        log.debug(s"Folder at ${f.flattenPath} will be created.")
        saveFolder(f)

      case fe: Boolean if fe && Path.root != f.flattenPath =>
        log.debug(s"Folder at ${f.flattenPath} will be updated.")
        f.metadata.fid.map(_ => updateFolder(f)).getOrElse {
          InvalidData(s"Can't update folder because it's missing FolderId")
        }

      case fe: Boolean if fe =>
        val msg = s"Folder at ${f.flattenPath} already exists."
        log.debug(msg)
        IllegalDestination(msg, f.metadata.path)

    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying persist a folder.", ex)
        Failed(ex.getMessage)
    }

  private[this] def moveBaseQry(implicit ctx: SymbioticContext) = {
    $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      IsDeletedKey.full $eq false,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
    )
  }

  private[this] def moveChildrenQry(orig: Path)(
      implicit ctx: SymbioticContext
  ) = $and(moveBaseQry, PathKey.full $eq Path.regex(orig))

  override def move(orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[MoveResult[Int]] =
    Future {
      val origQry = $and(
        moveBaseQry,
        PathKey.full $eq orig.materialize,
        IsFolderKey.full $eq true
      )
      val updOrig = $set(
        "filename"   -> mod.nameOfLast,
        PathKey.full -> mod.materialize
      )
      val origUpdated = collection.update(origQry, updOrig, upsert = false)

      if (origUpdated.getN == 1) {
        val childr =
          collection.find(moveChildrenQry(orig)).map(managedfileFromBSON)
        if (childr.nonEmpty) {
          Try {
            childr.map { f =>
              val newPath = f.flattenPath.replaceParent(orig, mod).materialize
              collection
                .update(
                  MongoDBObject("_id" -> f.id.get.toString),
                  $set(PathKey.full   -> newPath),
                  upsert = false,
                  multi = true
                )
                .getN
            }.sum
          } match {
            case scala.util.Success(n)  => Ok(n + 1)
            case scala.util.Failure(ex) => Failed(ex.getMessage)
          }
        } else {
          Ok(1)
        }
      } else {
        log.debug(s"Moving $orig to $mod changed nothing.")
        NotModified()
      }
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying move $orig to $mod.", ex)
        Failed(ex.getMessage)
    }

  override def lock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockResult[Lock]] =
    lockManagedFile(fid) {
      case (_, lock) =>
        Future {
          val qry = $and(
            FidKey.full $eq fid.value,
            OwnerIdKey.full $eq ctx.owner.id.value,
            IsFolderKey.full $eq true,
            IsDeletedKey.full $eq false,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value)
          )
          val upd = $set(LockKey.full -> lockToBSON(lock))
          if (collection.update(qry, upd).getN > 0) Ok(lock) else NotModified()
        }
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying lock folder $fid.", ex)
        Failed(ex.getMessage)
    }

  override def unlock(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[UnlockResult[Unit]] =
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
        if (res.getN > 0) Ok(()) else NotModified()
      }
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying unlock folder $fid.", ex)
        Failed(ex.getMessage)
    }

  override def editable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = Future {
    val qry = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      IsFolderKey.full $eq true,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      IsDeletedKey.full $eq false,
      $or(from.allPaths.map { p =>
        MongoDBObject(PathKey.full -> p.materialize)
      })
    )
    collection.find(qry).map(folderFromBSON).forall(_.metadata.lock.isEmpty)
  }

  override def markAsDeleted(fid: FolderId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Int]] =
    Future {
      val res = collection.update(
        $and(
          FidKey.full $eq fid.value,
          OwnerIdKey.full $eq ctx.owner.id.value,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
          IsFolderKey.full $eq true,
          IsDeletedKey.full $eq false
        ),
        $set(IsDeletedKey.full -> true)
      )

      log.debug(s"Got result: $res")

      if (res.getN > 0) Ok(res.getN)
      else NotModified()
    }.recover {
      case NonFatal(ex) =>
        log.error(s"An error occurred trying mark folder $fid as deleted.", ex)
        Failed(ex.getMessage)
    }

}
