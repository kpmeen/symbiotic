/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import core.lib.{Updated, Created}
import models.base.ShortName
import models.party.Organisation
import models.party.PartyBaseTypes.OrganisationId
import org.specs2.mutable.Specification
import repository.mongodb.party.MongoDBOrganisationRepository
import util.mongodb.MongoSpec

class OrganisationServiceSpec extends Specification with MongoSpec {

  def createOrganisation(sn: ShortName, n: String): Organisation =
    Organisation(
      id = OrganisationId.createOpt(),
      shortName = sn,
      name = n
    )

  "When using the OrganisationService it" should {
    "be possible to add a new Organisation" in {
      val org = createOrganisation(ShortName("FB1"), "Foo Bar1")
      MongoDBOrganisationRepository.save(org) must_== Created
    }

    "be possible to find an Organisation by OrganisationId" in {
      val org = createOrganisation(ShortName("FB2"), "Foo Bar2")
      MongoDBOrganisationRepository.save(org) must_== Created

      // scalastyle:off
      val actual = MongoDBOrganisationRepository.findById(org.id.get)
      actual must_!= None
      actual.get.name must_== org.name
      actual.get.shortName must_== org.shortName
      // scalastyle:on
    }

    "be possible to find an Organisation by ShortName" in {
      val org = createOrganisation(ShortName("FB3"), "Foo Bar3")
      MongoDBOrganisationRepository.save(org) must_== Created

      // scalastyle:off
      val actual = MongoDBOrganisationRepository.findByShortName(org.shortName)
      actual must_!= None
      actual.get.name must_== org.name
      actual.get.shortName must_== org.shortName
      // scalastyle:on
    }

    "be possible to update an Organisation" in {
      val org = createOrganisation(ShortName("FB4"), "Foo Bar4")
      MongoDBOrganisationRepository.save(org) must_== Created

      val res = MongoDBOrganisationRepository.findByShortName(org.shortName)
      res must_!= None

      val mod = res.get.copy(description = Some("This is a test")) // scalastyle:ignore
      MongoDBOrganisationRepository.save(mod) must_== Updated
    }
  }
}
