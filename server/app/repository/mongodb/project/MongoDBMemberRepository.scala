/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package repository.mongodb.project

import com.google.inject.Singleton
import com.mongodb.casbah.commons.MongoDBObject
import core.lib.{Created, Failure, SuccessOrFailure, Updated}
import models.base.Id
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.project.{Member, MemberId, ProjectId}
import org.slf4j.LoggerFactory
import repository.MemberRepository
import repository.mongodb.bson.BSONConverters.Implicits._
import repository.mongodb.{DefaultDB, WithMongoIndex}

import scala.util.Try

@Singleton
class MongoDBMemberRepository extends MemberRepository with DefaultDB with WithMongoIndex {

  val logger = LoggerFactory.getLogger(this.getClass)

  override val collectionName: String = "project_memberships"

  ensureIndex()

  override def ensureIndex(): Unit = index(List(
    Indexable("uid", unique = false),
    Indexable("pid", unique = false),
    Indexable("orgId", unique = false)
  ), collection)

  override def save(m: Member): SuccessOrFailure =
    Try {
      val res = collection.save(m)

      if (res.isUpdateOfExisting) Updated
      else Created
    }.recover {
      case t: Throwable =>
        logger.warn(s"Member could not be saved", t)
        throw t
    }.getOrElse(
      Failure(s"Member $m could not be saved")
    )

  override def findById(mid: MemberId): Option[Member] =
    collection.findOne(MongoDBObject("_id" -> mid.value)).map(member_fromBSON)

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
