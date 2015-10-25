/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.project

import com.mongodb.casbah.commons.MongoDBObject
import core.mongodb.{DefaultDB, WithMongoIndex}
import models.base.Id
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.project.{Member, MemberId, ProjectId}
import org.slf4j.LoggerFactory

import scala.util.Try

object MemberService extends DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(MemberService.getClass)

  override val collectionName: String = "project_memberships"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("id", unique = true),
    Indexable("uid", unique = false),
    Indexable("pid", unique = false),
    Indexable("orgId", unique = false)
  ), collection)

  def save(m: Member): Unit =
    Try {
      val res = collection.save(m)

      if (res.isUpdateOfExisting) logger.info("Updated existing project member")
      else logger.info("Inserted new project member")

      logger.debug(res.toString)
    }.recover {
      case t: Throwable => logger.warn(s"Member could not be saved", t)
    }

  def findById(mid: MemberId): Option[Member] =
    collection.findOne(MongoDBObject("id" -> mid.value)).map(Member.fromBSON)

  def findBy[A <: Id](id: A): Seq[Member] =
    id match {
      case uid: UserId => findByUserId(uid)
      case pid: ProjectId => findByProjectId(pid)
      case oid: OrganisationId => findByOrganisationId(oid)
      case mid: MemberId =>
        logger.error("Calling findBy[A <: Id](id: A): Seq[Member] with a MemberId. Use findById instead!")
        Seq.empty
      case unknown =>
        logger.warn(s"Did not recognize ID with type ${unknown.getClass} with value $unknown")
        Seq.empty

    }

  def findByUserId(uid: UserId): Seq[Member] =
    collection.find(MongoDBObject("uid" -> uid.value)).map(Member.fromBSON).toSeq

  def findByProjectId(pid: ProjectId): Seq[Member] =
    collection.find(MongoDBObject("pid" -> pid.value)).map(Member.fromBSON).toSeq

  def findByOrganisationId(oid: OrganisationId): Seq[Member] =
    collection.find(MongoDBObject("orgId" -> oid.value)).map(Member.fromBSON).toSeq
}
