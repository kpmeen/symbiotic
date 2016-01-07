/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.google.inject.{Inject, Singleton}
import core.lib.SuccessOrFailure
import models.base.ShortName
import models.party.Organisation
import models.party.PartyBaseTypes.OrganisationId
import repository.OrganisationRepository

@Singleton
class OrganisationService @Inject() (val orgRepo: OrganisationRepository) {

  def save(org: Organisation): SuccessOrFailure = orgRepo.save(org)

  def findById(id: OrganisationId): Option[Organisation] = orgRepo.findById(id)

  def findByShortName(sname: ShortName): Option[Organisation] = orgRepo.findByShortName(sname)

}
