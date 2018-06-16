package net.scalytica.symbiotic.mongodb.docmanagement

import java.util.UUID

import akka.stream.Materializer
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.GridFSDBFile
import com.mongodb.gridfs.{GridFSDBFile => MongoGridFSDBFile}
import com.typesafe.config.Config
import net.scalytica.symbiotic.api.SymbioticResults._
import net.scalytica.symbiotic.api.repository.FileRepository
import net.scalytica.symbiotic.api.types.MetadataKeys._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.mongodb.bson.BSONConverters.Implicits._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MongoDBFileRepository(
    val configuration: Config
)(implicit mat: Materializer)
    extends FileRepository
    with MongoFSRepository {

  private val log = LoggerFactory.getLogger(this.getClass)

  log.debug(
    s"Using configuration " + configuration.getConfig(
      "symbiotic.persistence.mongodb"
    )
  )

  private[this] def insertFile(f: File): SaveResult[FileId] = {
    val id   = UUID.randomUUID()
    val fid  = f.metadata.fid.getOrElse(FileId.create())
    val file = f.copy(metadata = f.metadata.copy(fid = Some(fid)))

    f.inputStream.map { s =>
      gfs(s) { gf =>
        gf.filename = file.filename
        file.fileType.foreach(gf.contentType = _)
        gf.metaData = managedmd_toBSON(file.metadata)
        gf += ("_id" -> id.toString)
      }.map(_ => Ok(fid)).getOrElse {
        Failed(s"Inserting new File failed")
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

      if (r.getN == 1) Ok(fid) else NotModified()
    }
  }

  private[this] def update(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[File]] = {
    (for {
      id  <- f.id
      fid <- f.metadata.fid
    } yield {
      val q = $and(
        "_id" $eq id.toString,
        OwnerIdKey.full $eq ctx.owner.id.value,
        AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
        IsFolderKey.full $eq false,
        IsDeletedKey.full $eq false
      )

      val upd = $set(MetadataKey -> managedmd_toBSON(f.metadata))

      val res = collection.update(q, upd, multi = false)

      if (res.getN > 0) {
        log.debug(s"Successfully updated $fid")
        findLatestBy(fid)
      } else {
        log.warn(
          s"Update of ${f.metadata.fid} named ${f.filename} didn't change" +
            " any data"
        )
        Future.successful(NotModified())
      }
    }).getOrElse {
      log.warn(
        s"Attempted update of ${f.filename} at ${f.metadata.path} without " +
          "providing its FileId and unique ID"
      )
      Future.successful(InvalidData("Missing FileId and/or Id"))
    }
  }

  override def updateMetadata(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[File]] =
    f.metadata.path.map { p =>
      if (isEditable(p)) {
        update(f)
      } else {
        log.warn(
          s"Can't update metadata for File ${f.filename} because $p is " +
            "not editable"
        )
        Future.successful(NotEditable("File is not editable"))
      }
    }.getOrElse {
      log.warn(
        s"Can't update metadata for File ${f.filename} without a " +
          "destination path"
      )
      Future.successful(InvalidData("Missing path"))
    }

  override def save(f: File)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[SaveResult[FileId]] = Future {
    f.metadata.path.map { p =>
      if (isEditable(p)) {
        log.debug(s"Going to insert ${f.filename}")
        insertFile(f)
      } else {
        log.warn(
          s"Can't save File ${f.filename} to $p because its not editable"
        )
        NotEditable("File is not editable")
      }
    }.getOrElse {
      log.warn(s"Can't save File ${f.filename} without a destination path")
      InvalidData("Missing path")
    }
  }

  private[this] def find(id: UUID)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[File]] =
    Future {
      gfs
        .findOne(
          MongoDBObject(
            "_id"            -> id.toString,
            IsFolderKey.full -> false
          )
        )
        .map(f => Ok(file_fromGridFS(f)))
        .getOrElse(NotFound())
    }.recover {
      case NonFatal(ex) => Failed(ex.getMessage)
    }

  override def findLatestBy(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[File]] = {
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
      .map(f => find(f.id.get))
      .getOrElse(Future.successful(NotFound()))
  }

  override def move(filename: String, orig: Path, mod: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[MoveResult[File]] = {
    findLatest(filename, Some(orig)).flatMap {
      case Ok(existing) =>
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
          else Future.successful(NotModified())

        } else {
          Future.successful(
            ResourceLocked(
              msg = "Resource is locked by another user",
              mby = existing.metadata.lock.map(_.by)
            )
          )
        }

      case fail =>
        Future.successful(fail)
    }
  }

  override def find(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[File]]] =
    Future {
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

      val res = gfs
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
      if (res.nonEmpty) Ok(res) else NotFound()
    }.recover {
      case NonFatal(ex) => Failed(ex.getMessage)
    }

  override def findLatest(filename: String, maybePath: Option[Path])(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[File]] = find(filename, maybePath).map { res =>
    res.flatMap(_.headOption.map(Ok.apply).getOrElse(NotFound()))
  }

  override def listFiles(path: Path)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[GetResult[Seq[File]]] = Future {
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
    Ok(res.groupBy(_.filename).map(_._2.maxBy(_.metadata.version)).toSeq)
  }

  override def lock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[LockResult[Lock]] = {
    lockManagedFile(fid) {
      case (dbId @ _, lock) =>
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
            Ok(lock)
          } else {
            log.warn("Locking query did not match any documents")
            NotModified()
          }
        }
    }
  }

  override def unlock(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[UnlockResult[Unit]] =
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
        if (res.getN > 0) Ok(())
        else {
          log.warn("Unlocking query did not match any documents")
          NotModified()
        }
      }
    }

  private def isEditable(
      from: Path
  )(implicit ctx: SymbioticContext): Boolean = {
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
  ): Future[DeleteResult[Int]] = Future {
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

    if (res.getN > 0) Ok(res.getN)
    else {
      log.warn(s"File $fid was not marked as deleted")
      NotModified()
    }
  }

  override def eraseFile(fid: FileId)(
      implicit ctx: SymbioticContext,
      ec: ExecutionContext
  ): Future[DeleteResult[Int]] = Future {
    val query = $and(
      FidKey.full $eq fid.value,
      OwnerIdKey.full $eq ctx.owner.id.value,
      AccessibleByIdKey.full $in ctx.accessibleParties.map(_.value),
      IsFolderKey.full $eq false
    )

    val numBefore = collection.find(query).size

    gfs.remove(query)

    val numAfter = gfs.find(query).size

    if (numAfter == 0) Ok(numBefore)
    else {
      log.warn(s"File $fid was not fully erased")
      NotModified()
    }
  }

}
