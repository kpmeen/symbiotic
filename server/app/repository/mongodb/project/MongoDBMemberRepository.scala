/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.project

import com.mongodb.casbah.commons.MongoDBObject
import models.base.Id
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.project.{Member, MemberId, ProjectId}
import org.slf4j.LoggerFactory
import repository.MemberRepository
import repository.mongodb.bson.BSONConverters.Implicits._
import repository.mongodb.{DefaultDB, WithMongoIndex}

import scala.util.Try

object MongoDBMemberRepository extends MemberRepository with DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(MongoDBMemberRepository.getClass)

  override val collectionName: String = "project_memberships"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("uid", unique = false),
    Indexable("pid", unique = false),
    Indexable("orgId", unique = false)
  ), collection)

  override def save(m: Member): Unit =
    Try {
      val res = collection.save(m)

      if (res.isUpdateOfExisting) logger.info("Updated existing project member")
      else logger.info("Inserted new project member")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"Member could not be saved", t)
    }

  override def findById(mid: MemberId): Option[Member] =
    collection.findOne(MongoDBObject("id" -> mid.value)).map(member_fromBSON)

  override def listBy[A <: Id](id: A): Seq[Member] =
    id match {
      case uid: UserId => listByUserId(uid)
      case pid: ProjectId => listByProjectId(pid)
      case oid: OrganisationId => listByOrganisationId(oid)
      case mid: MemberId =>
        logger.error("Calling findBy[A <: Id](id: A): Seq[Member] with a MemberId. Use findById instead!")
        Seq.empty
      case unknown =>
        logger.warn(s"Did not recognize ID with type ${unknown.getClass} with value $unknown")
        Seq.empty

    }

  override def listByUserId(uid: UserId): Seq[Member] =
    collection.find(MongoDBObject("uid" -> uid.value)).map(member_fromBSON).toSeq

  override def listByProjectId(pid: ProjectId): Seq[Member] =
    collection.find(MongoDBObject("pid" -> pid.value)).map(member_fromBSON).toSeq

  override def listByOrganisationId(oid: OrganisationId): Seq[Member] =
    collection.find(MongoDBObject("orgId" -> oid.value)).map(member_fromBSON).toSeq
}
