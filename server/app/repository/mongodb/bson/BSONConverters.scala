/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.bson

import java.util.{Date, UUID}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.DateTimeConverters
import models.base.PersistentType.{UserStamp, VersionStamp}
import models.base._
import models.docmanagement.MetadataKeys._
import models.docmanagement._
import models.party.PartyBaseTypes.UserId
import models.party.{Avatar, AvatarMetadata, User}

object BSONConverters {

  object Implicits
    extends UserStampBSONConverter
    with VersionStampBSONConverter
    with NameBSONConverter
    with UserBSONConverter
    with AvatarBSONConverter
    with FileFolderBSONConverter

  trait LockBSONConverter extends DateTimeConverters {
    implicit def lock_toBSON(lock: Lock): MongoDBObject = {
      MongoDBObject(
        "by" -> lock.by.value,
        "date" -> lock.date.toDate
      )
    }

    implicit def lock_fromBSON(dbo: MongoDBObject): Lock = {
      Lock(
        by = UserId.asId(dbo.as[String]("by")),
        date = dbo.as[java.util.Date]("date")
      )
    }
  }

  trait AvatarMetadataBSONConverter {
    implicit def avatarmd_toBSON(amd: AvatarMetadata): DBObject =
      MongoDBObject("uid" -> amd.uid.value)

    implicit def avatarmd_fromBSON(dbo: DBObject): AvatarMetadata =
      AvatarMetadata(UserId.asId(dbo.as[String]("uid")))
  }

  trait ManagedFileMetadataBSONConverter extends LockBSONConverter {

    implicit def managedfmd_toBSON(fmd: ManagedFileMetadata): DBObject = {
      val b = MongoDBObject.newBuilder
      fmd.owner.foreach(o => b += OwnerKey.key -> o.value)
      b += VersionKey.key -> fmd.version
      fmd.fid.foreach(b += "fid" -> _.value)
      b += IsFolderKey.key -> fmd.isFolder.getOrElse(false)
      fmd.uploadedBy.foreach(u => b += UploadedByKey.key -> u.value)
      fmd.description.foreach(d => b += DescriptionKey.key -> d)
      fmd.lock.foreach(l => b += LockKey.key -> lock_toBSON(l))
      fmd.path.foreach(f => b += PathKey.key -> f.materialize)

      b.result()
    }

    implicit def managedfmd_fromBSON(dbo: DBObject): ManagedFileMetadata = {
      ManagedFileMetadata(
        owner = dbo.getAs[String](OwnerKey.key).map(UserId.apply),
        fid = dbo.getAs[String](FidKey.key),
        uploadedBy = dbo.getAs[String](UploadedByKey.key),
        version = dbo.getAs[Int](VersionKey.key).getOrElse(1),
        isFolder = dbo.getAs[Boolean](IsFolderKey.key),
        path = dbo.getAs[String](PathKey.key).map(Path.apply),
        description = dbo.getAs[String](DescriptionKey.key),
        lock = dbo.getAs[MongoDBObject](LockKey.key).map(lock_fromBSON)
      )
    }
  }

  trait UserStampBSONConverter extends DateTimeConverters {
    implicit def userstamp_toBSON(x: UserStamp): DBObject =
      MongoDBObject(
        "date" -> x.date.toDate,
        "by" -> x.by.value
      )

    implicit def userstamp_fromBSON(dbo: DBObject): UserStamp =
      UserStamp(
        date = dbo.as[Date]("date"),
        by = UserId.asId(dbo.as[String]("by"))
      )
  }

  trait VersionStampBSONConverter extends UserStampBSONConverter {

    implicit def versionstamp_toBSON(x: VersionStamp): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "version" -> x.version
      x.created.foreach(b += "created" -> userstamp_toBSON(_))
      x.modified.foreach(b += "modified" -> userstamp_toBSON(_))

      b.result()
    }

    implicit def versionstamp_fromBSON(dbo: DBObject): VersionStamp =
      VersionStamp(
        version = dbo.getAsOrElse[Int]("version", 0),
        created = dbo.getAs[DBObject]("created").map(userstamp_fromBSON),
        modified = dbo.getAs[DBObject]("modified").map(userstamp_fromBSON)
      )
  }

  trait NameBSONConverter {
    implicit def name_toBSON(n: Name): DBObject = {
      val b = MongoDBObject.newBuilder
      n.first.foreach(f => b += "first" -> f)
      n.middle.foreach(m => b += "middle" -> m)
      n.last.foreach(l => b += "last" -> l)

      b.result()
    }

    implicit def name_fromBSON(dbo: DBObject): Name = {
      Name(
        first = dbo.getAs[String]("first"),
        middle = dbo.getAs[String]("middle"),
        last = dbo.getAs[String]("last")
      )
    }
  }

  trait LoginInfoBSONConverter {

    implicit def loginInfo_toBSON(li: LoginInfo): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "providerID" -> li.providerID
      b += "providerKey" -> li.providerKey

      b.result()
    }

    implicit def loginInfo_fromBSON(dbo: DBObject): LoginInfo = {
      LoginInfo(
        providerID = dbo.as[String]("providerID"),
        providerKey = dbo.as[String]("providerKey")
      )
    }
  }

  trait PasswordInfoBSONConverter {
    implicit def passwordInfo_toBSON(pi: PasswordInfo): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "hasher" -> pi.hasher
      b += "password" -> pi.password
      pi.salt.foreach(s => b += "salt" -> s)

      b.result()
    }

    implicit def passwordInfo_fromBSON(dbo: DBObject): PasswordInfo = {
      PasswordInfo(
        hasher = dbo.as[String]("hasher"),
        password = dbo.as[String]("password"),
        salt = dbo.getAs[String]("salt")
      )
    }
  }

  trait OAuth2InfoBSONConverter {
    implicit def oauth2Info_toBSON(oi: OAuth2Info): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "accessToken" -> oi.accessToken
      oi.tokenType.foreach(b += "tokenType" -> _)
      oi.expiresIn.foreach(b += "expiresIn" -> _)
      oi.refreshToken.foreach(b += "refreshToken" -> _)
      oi.params.foreach(p => b += "params" -> MongoDBObject(p.toArray: _*))

      b.result()
    }

    implicit def oauth2Info_fromBSON(dbo: DBObject): OAuth2Info = {
      OAuth2Info(
        accessToken = dbo.as[String]("accessToken"),
        tokenType = dbo.getAs[String]("tokenType"),
        expiresIn = dbo.getAs[Int]("expiresIn"),
        refreshToken = dbo.getAs[String]("refreshToken"),
        params = dbo.getAs[Map[String, String]]("params")
      )
    }
  }

  trait UserBSONConverter
      extends DateTimeConverters
      with VersionStampBSONConverter
      with NameBSONConverter
      with LoginInfoBSONConverter {

    implicit def user_toBSON(u: User): DBObject = {
      val b = MongoDBObject.newBuilder
      u.id.foreach(b += "_id" -> _.value)
      b += "loginInfo" -> loginInfo_toBSON(u.loginInfo)
      u.v.foreach(b += "v" -> versionstamp_toBSON(_))
      b += "username" -> u.username.value
      b += "email" -> u.email.adr
      u.name.foreach(b += "name" -> name_toBSON(_))
      u.dateOfBirth.foreach(d => b += "dateOfBirth" -> d.toDate)
      u.gender.foreach(g => b += "gender" -> g.value)
      b += "active" -> u.active
      u.avatarUrl.foreach(b += "avatarUrl" -> _)
      b += "useSocialAvatar" -> u.useSocialAvatar

      b.result()
    }

    implicit def user_fromBSON(d: DBObject): User = {
      User(
        id = d.getAs[String]("_id"),
        loginInfo = loginInfo_fromBSON(d.as[DBObject]("loginInfo")),
        v = d.getAs[DBObject]("v").map(versionstamp_fromBSON),
        username = Username(d.as[String]("username")),
        email = Email(d.as[String]("email")),
        name = d.getAs[DBObject]("name").map(name_fromBSON),
        dateOfBirth = asOptDateTime(d.getAs[Date]("dateOfBirth")),
        gender = d.getAs[String]("gender").flatMap(g => Gender.fromString(g)),
        active = d.getAs[Boolean]("active").getOrElse(true),
        avatarUrl = d.getAs[String]("avatarUrl"),
        useSocialAvatar = d.getAs[Boolean]("useSocialAvatar").getOrElse(true)
      )
    }
  }

  trait AvatarBSONConverter extends DateTimeConverters with AvatarMetadataBSONConverter {
    /**
     * Converter to map between a GridFSDBFile (from read operations) to an Avatar image
     *
     * @param gf GridFSDBFile
     * @return Avatar
     */
    implicit def avatar_fromGridFS(gf: GridFSDBFile): Avatar = {
      val md = gf.metaData
      Avatar(
        id = gf.getAs[String]("_id").map(UUID.fromString),
        filename = gf.filename.getOrElse("no_name"),
        contentType = gf.contentType,
        uploadDate = Option(asDateTime(gf.uploadDate)),
        length = Option(gf.length.toString),
        stream = Option(gf.inputStream),
        metadata = avatarmd_fromBSON(md)
      )
    }

    implicit def avatar_fromMaybeGridFS(mgf: Option[GridFSDBFile]): Option[Avatar] = mgf.map(avatar_fromGridFS)

    /**
     * Converter to map between a DBObject (from read operations) to a File.
     * This will typically be used when listing files in a GridFS <bucket>.files collection
     *
     * @param dbo DBObject
     * @return File
     */
    implicit def avatar_fromBSON(dbo: DBObject): Avatar = {
      val mdbo = new MongoDBObject(dbo)
      val md = mdbo.as[DBObject]("metadata")
      Avatar(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
        contentType = mdbo.getAs[String]("contentType"),
        uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
        length = mdbo.getAs[Long]("length").map(_.toString),
        stream = None,
        metadata = avatarmd_fromBSON(md)
      )
    }
  }

  trait FileFolderBSONConverter extends ManagedFileMetadataBSONConverter with DateTimeConverters {

    implicit def folder_fromBSON(dbo: DBObject): Folder = {
      val mdbo = new MongoDBObject(dbo)
      val md = mdbo.as[DBObject](MetadataKey)
      Folder(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.as[String]("filename"),
        metadata = managedfmd_fromBSON(md)
      )
    }

    /**
     * Converter to map between a GridFSDBFile (from read operations) to a File
     *
     * @param gf GridFSDBFile
     * @return File
     */
    implicit def file_fromGridFS(gf: GridFSDBFile): File = {
      File(
        id = gf.getAs[String]("_id").map(UUID.fromString),
        filename = gf.filename.getOrElse("no_name"),
        contentType = gf.contentType,
        uploadDate = Option(asDateTime(gf.uploadDate)),
        length = Option(gf.length.toString),
        stream = Option(gf.inputStream),
        metadata = managedfmd_fromBSON(gf.metaData)
      )
    }

    implicit def file_fromMaybeGridFS(mgf: Option[GridFSDBFile]): Option[File] = mgf.map(file_fromGridFS)

    /**
     * Converter to map between a DBObject (from read operations) to a File.
     * This will typically be used when listing files in a GridFS <bucket>.files collection
     *
     * @param dbo DBObject
     * @return File
     */
    implicit def file_fromBSON(dbo: DBObject): File = {
      val mdbo = new MongoDBObject(dbo)
      val md = mdbo.as[DBObject](MetadataKey)
      File(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
        contentType = mdbo.getAs[String]("contentType"),
        uploadDate = mdbo.getAs[java.util.Date]("uploadDate"),
        length = mdbo.getAs[Long]("length").map(_.toString),
        stream = None,
        metadata = managedfmd_fromBSON(md)
      )
    }

    implicit def managedfile_fromBSON(dbo: DBObject): ManagedFile = {
      val isFolder = dbo.getAs[Boolean](IsFolderKey.full).getOrElse(false)
      if (isFolder) folder_fromBSON(dbo)
      else file_fromBSON(dbo)
    }
  }

}
