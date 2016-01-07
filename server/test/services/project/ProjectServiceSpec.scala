/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.project

import core.lib.{Created, Updated}
import models.party.PartyBaseTypes.OrganisationId
import models.project.{Project, ProjectId}
import org.specs2.mutable.Specification
import repository.mongodb.project.MongoDBProjectRepository
import util.mongodb.MongoSpec

class ProjectServiceSpec extends Specification with MongoSpec {

  val service = new ProjectService(new MongoDBProjectRepository())

  def createProject(oid: OrganisationId, title: String): Project =
    Project(
      v = None,
      id = ProjectId.createOpt(),
      oid = oid,
      title = title,
      description = None
    )

  "When using the ProjectService it" should {
    "be possible to add a new Project" in {
      val prj = createProject(OrganisationId.create(), "Test project 1")
      service.save(prj) must_== Created
    }

    "be possible to find a Project by ProjectId" in {
      val prj = createProject(OrganisationId.create(), "Test project 2")
      service.save(prj) must_== Created

      val actual = service.findById(prj.id.get) // scalastyle:ignore
      actual must_!= None
      // scalastyle:off
      actual.get.id must_== prj.id
      actual.get.title must_== prj.title
      actual.get.description must_== prj.description
      // scalastyle:on
    }

    "be possible to find all Projects for an OrgId" in {
      val oid = OrganisationId.create()

      service.save(createProject(oid, "Test project 3a")) must_== Created
      service.save(createProject(oid, "Test project 3b")) must_== Created
      service.save(createProject(oid, "Test project 3c")) must_== Created

      val actual = service.listByOrgId(oid)
      actual.size must_== 3
    }

    "be possible to update a Project" in {
      val prj1 = createProject(OrganisationId.create(), "Test project 4")
      service.save(prj1) must_== Created

      val prj2 = service.findById(prj1.id.get) // scalastyle:ignore
      prj2 must_!= None

      val mod = prj2.get.copy(description = Some("Foo description")) // scalastyle:ignore

      service.save(mod) must_== Updated

      val prj3 = service.findById(prj1.id.get) // scalastyle:ignore
      prj3 must_!= None
      prj3.get.description must_== mod.description // scalastyle:ignore
    }
  }

}
