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
import org.joda.time.DateTime
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

  private[this] def insertFile(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Option[FileId] = {
    val id   = UUID.randomUUID()
    val fid  = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))
    Try {
      f.inputStream.map { s =>
        gfs(s) { gf =>
          gf.filename = file.filename
          file.fileType.foreach(gf.contentType = _)
          gf.metaData = managedmd_toBSON(file.metadata)
          gf += ("_id" -> id.toString)
        }
      }.getOrElse {
        val mdBson: DBObject = f.metadata
        val ctype = f.fileType
          .map(t => MongoDBObject("contentType" -> t))
          .getOrElse(MongoDBObject.empty)
        val dbo = MongoDBObject(
          "_id"        -> id.toString,
          "filename"   -> f.filename,
          "uploadDate" -> f.createdDate.getOrElse(DateTime.now()).toDate,
          MetadataKey  -> mdBson
        ) ++ ctype

        val r = collection.save(dbo)

        log.debug(s"Result from save was ${r.getN}")

        if (r.getN == 1) Some(fid) else None
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
        log.debug(s"Going to insert ${f.filename}")
        insertFile(f)
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
          IsFolderKey.full $eq false,
          IsDeletedKey.full $eq false
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
            IsFolderKey.full $eq false,
            IsDeletedKey.full $eq false
          )
          val upd = $set(PathKey.full -> mod.materialize)

          val res = collection.update(q, upd, multi = true)

          if (res.getN > 0) findLatest(filename, Some(mod))
          else
            Future.successful(None) // TODO: Handle this situation properly...

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
      IsFolderKey.full $eq false,
      IsDeletedKey.full $eq false
    )
    val query = maybePath.fold(fn) { p =>
      fn ++ MongoDBObject(PathKey.full -> p.materialize)
    }
    val sort = MongoDBObject(VersionKey.full -> -1)

    gfs
      .files(query)
      .sort(sort)
      .collect[File] {
        case f: DBObject =>
          val gf = f.asInstanceOf[MongoGridFSDBFile]
          log.debug(s"Number of file chunks: ${gf.numChunks()}")
          if (gf.numChunks() > 0) new GridFSDBFile(gf)
          else new GridFSDBFile(gf).copy(stream = None)
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
          IsFolderKey.full $eq false,
          IsDeletedKey.full $eq false
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
            IsFolderKey.full $eq false,
            IsDeletedKey.full $eq false
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
      IsDeletedKey.full $eq false,
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

  override def markAsDeleted(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Either[String, Int]] = Future {
    val res = collection.update(
      $and(
        FidKey.full $eq fid.value,
        OwnerIdKey.full $eq ctx.owner.id.value,
        AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
        IsFolderKey.full $eq false,
        IsDeletedKey.full $eq false
      ),
      $set(IsDeletedKey.full -> true)
    )

    log.debug(s"Got result: $res")

    if (res.getN > 0) Right(res.getN)
    else Left(s"File $fid was not marked as deleted")
  }

  override def eraseFile(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[Either[String, Int]] = Future {
    val query = $and(
      FidKey.full $eq fid.value,
      OwnerIdKey.full $eq ctx.owner.id.value,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      IsFolderKey.full $eq false
    )

    val numBefore = collection.find(query).size

    gfs.remove(query)

    val numAfter = gfs.find(query).size

    if (numAfter == 0) Right(numBefore)
    else Left(s"File $fid was not fully erased")
  }

}
