/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.bson

import java.util.Date

import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.gridfs.GridFSDBFile
import core.converters.DateTimeConverters
import core.security.authorisation.{ACLEntry, Permission, ACL, Role}
import models.base.PersistentType.{UserStamp, VersionStamp}
import models.base._
import models.docmanagement.MetadataKeys._
import models.docmanagement._
import models.party.PartyBaseTypes.UserId
import models.party.{Avatar, AvatarMetadata, Organisation, User}
import models.project.{Member, Project}
import org.bson.types.ObjectId

object BSONConverters {

  object Implicits
    extends UserStampBSONConverter
    with VersionStampBSONConverter
    with NameBSONConverter
    with UserBSONConverter
    with OrganisationBSONConverter
    with ProjectBSONConverter
    with MemberBSONConverter
    with AvatarBSONConverter
    with FileFolderBSONConverter
    with ACLBSONConverter

  trait ACLBSONConverter {
    implicit def aclentry_toBSON(ace: ACLEntry): DBObject = {
      val builder = MongoDBObject.newBuilder
      builder += "principal" -> ace.principal.value
      builder += "permissions" -> ace.permissions.map(Permission.asString)
      builder.result()
    }

    def aclentry_fromBSON(dbo: DBObject): ACLEntry =
      ACLEntry(
        principal = UserId(dbo.as[String]("principal")),
        permissions = dbo.as[MongoDBList]("permissions")
          .map(p => Permission.fromString(p.asInstanceOf[String]))
          .filter(_.isDefined)
          .map(_.get)
          .toSet
      )

    implicit def acl_toBSON(x: ACL): DBObject =
      MongoDBObject("entries" -> x.entries.map(ace => aclentry_toBSON(ace)))

    implicit def acl_fromBSON(dbo: DBObject): ACL =
      ACL(dbo.as[MongoDBList]("entries").map(dbo => aclentry_fromBSON(dbo.asInstanceOf[DBObject])))
  }

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

  trait ManagedFileMetadataBSONConverter extends LockBSONConverter with ACLBSONConverter {

    implicit def managedfmd_toBSON(fmd: ManagedFileMetadata): DBObject = {
      val b = MongoDBObject.newBuilder
      b += OidKey.key -> fmd.oid.value
      b += VersionKey.key -> fmd.version
      fmd.fid.foreach(b += "fid" -> _.value)
      b += IsFolderKey.key -> fmd.isFolder.getOrElse(false)
      fmd.uploadedBy.foreach(u => b += UploadedByKey.key -> u.value)
      fmd.description.foreach(d => b += DescriptionKey.key -> d)
      fmd.lock.foreach(l => b += LockKey.key -> lock_toBSON(l))
      fmd.path.foreach(f => b += PathKey.key -> f.materialize)
      fmd.pid.foreach(p => b += PidKey.key -> p.value)
      fmd.acl.foreach(a => b += AclKey.key -> acl_toBSON(a))

      b.result()
    }

    implicit def managedfmd_fromBSON(dbo: DBObject): ManagedFileMetadata = {
      ManagedFileMetadata(
        oid = dbo.as[String](OidKey.key),
        pid = dbo.getAs[String](PidKey.key),
        fid = dbo.getAs[String](FidKey.key),
        uploadedBy = dbo.getAs[String](UploadedByKey.key),
        version = dbo.getAs[Int](VersionKey.key).getOrElse(1),
        isFolder = dbo.getAs[Boolean](IsFolderKey.key),
        path = dbo.getAs[String](PathKey.key).map(Path.apply),
        description = dbo.getAs[String](DescriptionKey.key),
        lock = dbo.getAs[MongoDBObject](LockKey.key).map(lock_fromBSON),
        acl = dbo.getAs[DBObject](AclKey.key).map(acl_fromBSON)
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

  trait UserBSONConverter extends DateTimeConverters with VersionStampBSONConverter with NameBSONConverter {
    implicit def user_toBSON(u: User): DBObject = {
      val b = MongoDBObject.newBuilder
      u._id.foreach(b += "_id" -> _)
      u.v.foreach(b += "v" -> versionstamp_toBSON(_))
      u.id.foreach(b += "id" -> _.value)
      b += "username" -> u.username.value
      b += "email" -> u.email.adr
      b += "password" -> u.password.value
      u.name.foreach(b += "name" -> name_toBSON(_))
      u.dateOfBirth.foreach(d => b += "dateOfBirth" -> d.toDate)
      u.gender.foreach(g => b += "gender" -> g.value)
      b += "active" -> u.active

      b.result()
    }

    implicit def user_fromBSON(d: DBObject): User = {
      User(
        _id = d.getAs[ObjectId]("_id"),
        v = d.getAs[DBObject]("v").map(versionstamp_fromBSON),
        id = d.getAs[String]("id"),
        username = Username(d.as[String]("username")),
        email = Email(d.as[String]("email")),
        password = d.getAs[String]("password").map(Password.apply).getOrElse(Password.empty),
        name = d.getAs[DBObject]("name").map(name_fromBSON),
        dateOfBirth = asOptDateTime(d.getAs[Date]("dateOfBirth")),
        gender = d.getAs[String]("gender").flatMap(g => Gender.fromString(g)),
        active = d.getAs[Boolean]("active").getOrElse(true)
      )
    }
  }

  trait OrganisationBSONConverter extends DateTimeConverters with VersionStampBSONConverter {
    implicit def org_fromBSON(dbo: DBObject): Organisation =
      Organisation(
        _id = dbo.getAs[ObjectId]("_id"),
        v = dbo.getAs[DBObject]("v").map(versionstamp_fromBSON),
        id = dbo.getAs[String]("id"),
        shortName = ShortName(dbo.as[String]("shortName")),
        name = dbo.as[String]("name"),
        description = dbo.getAs[String]("description"),
        hasLogo = dbo.getAs[Boolean]("hasLogo").getOrElse(false)
      )

    implicit def org_toBSON(org: Organisation): DBObject = {
      val b = MongoDBObject.newBuilder
      org._id.foreach(b += "_id" -> _)
      org.v.foreach(b += "v" -> versionstamp_toBSON(_))
      org.id.foreach(b += "id" -> _.value)
      b += "shortName" -> org.shortName.code
      b += "name" -> org.name
      org.description.foreach(b += "description" -> _)
      b += "hasLogo" -> org.hasLogo

      b.result()
    }
  }

  trait ProjectBSONConverter extends DateTimeConverters with VersionStampBSONConverter {

    implicit def proj_toBSON(p: Project): DBObject = {
      val b = MongoDBObject.newBuilder
      p._id.foreach(b += "_id" -> _)
      p.v.foreach(b += "v" -> versionstamp_toBSON(_))
      p.id.foreach(b += "id" -> _.value)
      b += "oid" -> p.oid.value
      b += "title" -> p.title
      p.description.foreach(b += "description" -> _)
      p.startDate.foreach(b += "startDate" -> _.toDate)
      p.endDate.foreach(b += "endDate" -> _.toDate)
      b += "hasLogo" -> p.hasLogo

      b.result()
    }

    implicit def proj_fromBSON(d: DBObject): Project = {
      Project(
        _id = d.getAs[ObjectId]("_id"),
        v = d.getAs[DBObject]("v").map(versionstamp_fromBSON),
        id = d.getAs[String]("id"),
        oid = d.as[String]("oid"),
        title = d.as[String]("title"),
        description = d.getAs[String]("description"),
        startDate = d.getAs[java.util.Date]("startDate"),
        endDate = d.getAs[java.util.Date]("endDate"),
        hasLogo = d.getAs[Boolean]("hasLogo").getOrElse(false)
      )
    }
  }

  trait MemberBSONConverter extends VersionStampBSONConverter {
    implicit def member_toBSON(m: Member): DBObject = {
      val b = MongoDBObject.newBuilder

      m._id.foreach(b += "_id" -> _)
      m.v.foreach(b += "v" -> versionstamp_toBSON(_))
      m.id.foreach(b += "id" -> _.value)
      b += "uid" -> m.uid.value
      b += "uname" -> m.uname.value
      b += "orgId" -> m.orgId.value
      b += "pid" -> m.pid.value
      m.represents.foreach(b += "represents" -> _.value)
      b += "roles" -> m.roles.map(Role.toStringValue)

      b.result()
    }

    implicit def member_fromBSON(d: DBObject): Member = {
      Member(
        _id = d.getAs[ObjectId]("_id"),
        v = d.getAs[DBObject]("v").map(versionstamp_fromBSON),
        id = d.getAs[String]("id"),
        uid = d.as[String]("uid"),
        uname = Username(d.as[String]("uname")),
        orgId = d.as[String]("orgId"),
        pid = d.as[String]("pid"),
        represents = d.getAs[String]("represents"),
        roles = d.as[Seq[String]]("roles").map(Role.fromStringValue)
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
        id = gf._id,
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
        id = mdbo._id,
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
        id = mdbo._id,
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
        id = gf._id,
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
        id = mdbo._id,
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
