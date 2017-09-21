package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import com.mongodb.gridfs.{GridFSDBFile => MongoGridFSDBFile}
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.repository.FileRepository
import net.scalytica.symbiotic.api.types.Lock.LockOpStatusTypes._
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class MongoDBFileRepository(
    val configuration: Config
)(implicit as: ActorSystem, mat: Materializer)
    extends FileRepository
    with MongoFSRepository {

  private val log = LoggerFactory.getLogger(this.getClass)

  log.debug(
    s"Using configuration ${configuration.getConfig("symbiotic.mongodb")}"
  )

  private[this] def exists(maybeFid: Option[FileId], v: Version)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Boolean = {
    maybeFid.exists { fid =>
      collection
        .findOne(
          $and(
            FidKey.full $eq fid.value,
            OwnerIdKey.full $eq ctx.owner.id.value,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
            VersionKey.full $eq v,
            IsFolderKey.full $eq false
          )
        )
        .nonEmpty
    }
  }

  private def updateFile(fid: FileId, f: File)(
      implicit ctx: SymbioticContext
  ): Option[FileId] = {
    val setBuildr    = Seq.newBuilder[(String, Any)]
    val unsetBuilder = Seq.newBuilder[String]

    f.metadata.description.fold[Unit](unsetBuilder += DescriptionKey.full) {
      d =>
        setBuildr += DescriptionKey.full -> d
    }
    f.metadata.extraAttributes
      .fold[Unit](unsetBuilder += ExtraAttributesKey.full) { ea =>
        setBuildr += ExtraAttributesKey.full -> extraAttribs_toBSON(ea)
      }

    val set = $set(setBuildr.result: _*)
    val unset =
      if (unsetBuilder.result.nonEmpty) $unset(unsetBuilder.result: _*)
      else MongoDBObject.empty

    val res = collection.update(
      $and(FidKey.full $eq fid.value),
      set ++ unset
    )

    if (res.getN == 0) f.metadata.fid else None
  }

  private def insertFile(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Option[FileId] = {
    val id   = UUID.randomUUID()
    val fid  = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))
    Try {
      f.inputStream.flatMap { s =>
        gfs(s) { gf =>
          gf.filename = file.filename
          file.fileType.foreach(gf.contentType = _)
          gf.metaData = managedmd_toBSON(file.metadata)
          gf += ("_id" -> id.toString)
        }
      }.map(_ => fid)
    }.recover {
      case NonFatal(e) =>
        log.error(s"An error occurred trying to save $f", e)
        None
    }.toOption.flatten
  }

  override def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[FileId]] = Future {
    f.metadata.path.map { p =>
      if (isEditable(p)) {
        f.id.map { id =>
          find(id).flatMap { existing =>
            f.metadata.fid.map { fid =>
              val lockOk =
                existing.metadata.lock.forall(_.by == ctx.currentUser)
              if (lockOk) {
                log.debug(s"Going to update ${f.filename}")
                updateFile(fid, f)
              } else {
                log.warn(s"Updating ${f.filename} failed due to Missing FileId")
                None
              }
            }.getOrElse {
              log.warn(
                s"Update of file ${f.filename} because it has no FileId"
              )
              None
            }
          }
        }.getOrElse {
          log.debug(s"Going to insert ${f.filename}")
          insertFile(f)
        }
      } else {
        log.warn(
          s"Can't save File ${f.filename} to $p because its not editable"
        )
        None
      }
    }.getOrElse {
      log.warn(s"Can't save File ${f.filename} without a destination path")
      None
    }
  }

  private[this] def find(id: UUID)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Option[File] = {
    gfs.findOne(
      MongoDBObject(
        "_id"            -> id.toString,
        IsFolderKey.full -> false
      )
    )
  }

  override def findLatestBy(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    log.debug(s"Attempting to locate $fid")
    collection
      .find(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
          FidKey.full $eq fid.value,
          IsFolderKey.full $eq false
        )
      )
      .sort(MongoDBObject(VersionKey.full -> -1))
      .map { dbo =>
        log.debug(dbo.toString)
        managedfile_fromBSON(dbo)
      }
      .toSeq
      .headOption
      .map(f => Future(find(f.id.get)))
      .getOrElse(Future.successful(None))
  }

  override def move(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = {
    findLatest(filename, Some(orig)).flatMap {
      case Some(existing) =>
        if (existing.metadata.lock.forall(_.by == ctx.currentUser)) {
          val q = $and(
            "filename" $eq filename,
            OwnerIdKey.full $eq ctx.owner.id.value,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
            PathKey.full $eq orig.materialize,
            IsFolderKey.full $eq false
          )
          val upd = $set(PathKey.full -> mod.materialize)

          val res = collection.update(q, upd, multi = true)

          if (res.getN > 0) findLatest(filename, Some(mod))
          else Future.successful(None) // TODO: Handle this situation properly...

        } else {
          Future.successful(None)
        }

      case None =>
        Future.successful(None)
    }
  }

  override def find(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]] = Future {
    val fn = $and(
      "filename" $eq filename,
      OwnerIdKey.full $eq ctx.owner.id.value,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      IsFolderKey.full $eq false
    )
    val query = maybePath.fold(fn) { p =>
      fn ++ MongoDBObject(PathKey.full -> p.materialize)
    }
    val sort = MongoDBObject(VersionKey.full -> -1)

    gfs
      .files(query)
      .sort(sort)
      .collect[File] {
        case f: DBObject => new GridFSDBFile(f.asInstanceOf[MongoGridFSDBFile])
      }
      .toSeq
  }

  override def findLatest(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Option[File]] = find(filename, maybePath).map(_.headOption)

  override def listFiles(path: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Seq[File]] = Future {
    val res: Seq[File] = gfs
      .files(
        $and(
          OwnerIdKey.full $eq ctx.owner.id.value,
          AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
          PathKey.full $eq path.materialize,
          IsFolderKey.full $eq false
        )
      )
      .sort(DBObject("filename" -> 1, VersionKey.full -> -1))
      .toSeq
    // Only keep the highest version for each file
    res.groupBy(_.filename).map(_._2.maxBy(_.metadata.version)).toSeq
  }

  override def lock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: Option[Lock]]] = {
    lockManagedFile(fid) {
      case (dbId, lock) =>
        Future {
          val qry = $and(
            FidKey.full $eq fid.value,
            OwnerIdKey.full $eq ctx.owner.id.value,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
            IsFolderKey.full $eq false
          )
          val upd = $set(LockKey.full -> lock_toBSON(lock))
          if (collection.update(qry, upd).getN > 0) {
            LockApplied(Option(lock))
          } else {
            val msg = "Locking query did not match any documents"
            log.warn(msg)
            LockError(msg)
          }
        }
    }
  }

  override def unlock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockOpStatus[_ <: String]] =
    unlockManagedFile(fid) { dbId =>
      Future {
        val res = collection.update(
          $and(
            "_id" $eq dbId.toString,
            OwnerIdKey.full $eq ctx.owner.id.value,
            AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
            IsFolderKey.full $eq false
          ),
          $unset(LockKey.full)
        )
        if (res.getN > 0) LockRemoved(s"Successfully unlocked $fid")
        else LockError("Unlocking query did not match any documents")
      }
    }

  private def isEditable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Boolean = {
    val qry = $and(
      OwnerIdKey.full $eq ctx.owner.id.value,
      IsFolderKey.full $eq true,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      $or(from.allPaths.map { p =>
        MongoDBObject(PathKey.full -> p.materialize)
      })
    )
    val res = collection.find(qry).map(folder_fromBSON)
    if (res.isEmpty) false
    else res.forall(_.metadata.lock.isEmpty)
  }

  override def editable(from: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Boolean] = Future(isEditable(from))

}
