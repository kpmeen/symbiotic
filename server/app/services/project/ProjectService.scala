/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.project

import com.google.inject.{Inject, Singleton}
import core.lib.SuccessOrFailure
import models.party.PartyBaseTypes.OrganisationId
import models.project.{Project, ProjectId}
import repository.mongodb.project.MongoDBProjectRepository

@Singleton
class ProjectService @Inject() (val projectRepository: MongoDBProjectRepository) {

  def save(proj: Project): SuccessOrFailure = projectRepository.save(proj)

  def findById(pid: ProjectId): Option[Project] = projectRepository.findById(pid)

  def listByOrgId(oid: OrganisationId): Seq[Project] = projectRepository.listByOrgId(oid)

}
