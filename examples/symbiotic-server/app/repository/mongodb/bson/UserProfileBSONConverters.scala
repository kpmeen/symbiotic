package repository.mongodb.bson

import java.util.{Date, UUID}

import akka.stream.scaladsl.StreamConverters
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mongodb.DBObject
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.gridfs.GridFSDBFile
import models.base._
import models.party.{Avatar, AvatarMetadata, User}
import net.scalytica.symbiotic.mongodb.bson.BaseBSONConverters.{
  DateTimeBSONConverter,
  VersionStampBSONConverter
}

object UserProfileBSONConverters {

  object Implicits
      extends NameBSONConverter
      with UserBSONConverter
      with AvatarBSONConverter

  trait AvatarMetadataBSONConverter {
    implicit def avatarmdToBSON(amd: AvatarMetadata): DBObject =
      MongoDBObject("uid" -> amd.uid.value)

    implicit def avatarmdFromBSON(dbo: DBObject): AvatarMetadata =
      AvatarMetadata(SymbioticUserId.asId(dbo.as[String]("uid")))
  }

  trait NameBSONConverter {
    implicit def nameToBSON(n: Name): DBObject = {
      val b = MongoDBObject.newBuilder
      n.first.foreach(f => b += "first"   -> f)
      n.middle.foreach(m => b += "middle" -> m)
      n.last.foreach(l => b += "last"     -> l)

      b.result()
    }

    implicit def nameFromBSON(dbo: DBObject): Name = {
      Name(
        first = dbo.getAs[String]("first"),
        middle = dbo.getAs[String]("middle"),
        last = dbo.getAs[String]("last")
      )
    }
  }

  trait LoginInfoBSONConverter {

    implicit def loginInfoToBSON(li: LoginInfo): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "providerID"  -> li.providerID
      b += "providerKey" -> li.providerKey

      b.result()
    }

    implicit def loginInfoFromBSON(dbo: DBObject): LoginInfo = {
      LoginInfo(
        providerID = dbo.as[String]("providerID"),
        providerKey = dbo.as[String]("providerKey")
      )
    }
  }

  trait PasswordInfoBSONConverter {
    implicit def passwordInfoToBSON(pi: PasswordInfo): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "hasher"   -> pi.hasher
      b += "password" -> pi.password
      pi.salt.foreach(s => b += "salt" -> s)

      b.result()
    }

    implicit def passwordInfoFromBSON(dbo: DBObject): PasswordInfo = {
      PasswordInfo(
        hasher = dbo.as[String]("hasher"),
        password = dbo.as[String]("password"),
        salt = dbo.getAs[String]("salt")
      )
    }
  }

  trait OAuth2InfoBSONConverter {
    implicit def oauth2InfoToBSON(oi: OAuth2Info): DBObject = {
      val b = MongoDBObject.newBuilder
      b += "accessToken" -> oi.accessToken
      oi.tokenType.foreach(b += "tokenType"       -> _)
      oi.expiresIn.foreach(b += "expiresIn"       -> _)
      oi.refreshToken.foreach(b += "refreshToken" -> _)
      oi.params.foreach(p => b += "params" -> MongoDBObject(p.toArray: _*))

      b.result()
    }

    implicit def oauth2InfoFromBSON(dbo: DBObject): OAuth2Info = {
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
      extends DateTimeBSONConverter
      with VersionStampBSONConverter
      with NameBSONConverter
      with LoginInfoBSONConverter {

    implicit def userToBSON(u: User): DBObject = {
      val b = MongoDBObject.newBuilder
      u.id.foreach(b += "_id" -> _.value)
      b += "loginInfo" -> loginInfoToBSON(u.loginInfo)
      u.v.foreach(b += "v" -> versionstampToBSON(_))
      b += "username" -> u.username.value
      b += "email"    -> u.email.adr
      u.name.foreach(b += "name" -> nameToBSON(_))
      u.dateOfBirth.foreach(d => b += "dateOfBirth" -> d.toDate)
      u.gender.foreach(g => b += "gender"           -> g.value)
      b += "active" -> u.active
      u.avatarUrl.foreach(b += "avatarUrl" -> _)
      b += "useSocialAvatar" -> u.useSocialAvatar

      b.result()
    }

    implicit def userFromBSON(d: DBObject): User =
      User(
        id = d.getAs[String]("_id").map(SymbioticUserId.apply),
        loginInfo = loginInfoFromBSON(d.as[DBObject]("loginInfo")),
        v = d.getAs[DBObject]("v").map(versionstampFromBSON),
        username = Username(d.as[String]("username")),
        email = Email(d.as[String]("email")),
        name = d.getAs[DBObject]("name").map(nameFromBSON),
        dateOfBirth = asOptDateTime(d.getAs[Date]("dateOfBirth")),
        gender = d.getAs[String]("gender").flatMap(g => Gender.fromString(g)),
        active = d.getAs[Boolean]("active").getOrElse(true),
        avatarUrl = d.getAs[String]("avatarUrl"),
        useSocialAvatar = d.getAs[Boolean]("useSocialAvatar").getOrElse(true)
      )
  }

  trait AvatarBSONConverter
      extends DateTimeBSONConverter
      with AvatarMetadataBSONConverter {

    /**
     * Converter to map between a GridFSDBFile (from read operations) to an
     * Avatar image
     *
     * @param gf GridFSDBFile
     * @return Avatar
     */
    implicit def avatarFromGridFS(gf: GridFSDBFile): Avatar = {
      val md = gf.metaData
      Avatar(
        id = gf.getAs[String]("_id").map(UUID.fromString),
        filename = gf.filename.getOrElse("no_name"),
        fileType = gf.contentType,
        createdDate = Option(asDateTime(gf.uploadDate)),
        length = Option(gf.length.toString),
        stream = Option(StreamConverters.fromInputStream(() => gf.inputStream)),
        metadata = avatarmdFromBSON(md)
      )
    }

    implicit def avatarFromMaybeGridFS(
        mgf: Option[GridFSDBFile]
    ): Option[Avatar] =
      mgf.map(avatarFromGridFS)

    /**
     * Converter to map between a DBObject (from read operations) to a File.
     * This will typically be used when listing files in a GridFS <bucket>.files
     * collection
     *
     * @param dbo DBObject
     * @return File
     */
    implicit def avatarFromBSON(dbo: DBObject): Avatar = {
      val mdbo = new MongoDBObject(dbo)
      val md   = mdbo.as[DBObject]("metadata")
      Avatar(
        id = mdbo.getAs[String]("_id").map(UUID.fromString),
        filename = mdbo.getAs[String]("filename").getOrElse("no_name"),
        fileType = mdbo.getAs[String]("contentType"),
        createdDate = mdbo.getAs[java.util.Date]("uploadDate"),
        length = mdbo.getAs[Long]("length").map(_.toString),
        stream = None,
        metadata = avatarmdFromBSON(md)
      )
    }
  }

}
