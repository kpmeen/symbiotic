/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.project

import core.lib.SuccessOrFailure
import models.party.PartyBaseTypes.OrganisationId
import models.project.{Project, ProjectId}
import repository.mongodb.project.MongoDBProjectRepository

object ProjectService {

  def save(proj: Project): SuccessOrFailure = MongoDBProjectRepository.save(proj)

  def findById(pid: ProjectId): Option[Project] = MongoDBProjectRepository.findById(pid)

  def listByOrgId(oid: OrganisationId): Seq[Project] = MongoDBProjectRepository.listByOrgId(oid)

}
