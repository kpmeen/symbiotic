/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package repository

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import repository.mongodb.docmanagement._
import repository.mongodb.party._
import repository.mongodb.project._

object RepositoryTypeNames {
  val MongoRepo = "mongodb"
  val PqsqlRepo = "postgresql"
}

class MongoDBModule(
    environment: Environment,
    configuration: Configuration
) extends AbstractModule {

  def configure() = {
    val impl: String = configuration.getString("symbiotic.repository.type").getOrElse(RepositoryTypeNames.MongoRepo)

    bind(classOf[UserRepository]).to(classOf[MongoDBUserRepository])
    bind(classOf[OrganisationRepository]).to(classOf[MongoDBOrganisationRepository])

    bind(classOf[ProjectRepository]).to(classOf[MongoDBProjectRepository])
    bind(classOf[MemberRepository]).to(classOf[MongoDBMemberRepository])

    bind(classOf[FolderRepository]).to(classOf[MongoDBFolderRepository])
    bind(classOf[AvatarRepository]).to(classOf[MongoDBAvatarRepository])
    bind(classOf[FileRepository]).to(classOf[MongoDBFileRepository])
    bind(classOf[FSTreeRepository]).to(classOf[MongoDBFSTreeRepository])

  }

}
