/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package repository

import com.google.inject.AbstractModule
import net.scalytica.symbiotic.api.persistence.{
  FSTreeRepository,
  FileRepository,
  FolderRepository
}
import net.scalytica.symbiotic.mongodb.docmanagement.{
  MongoDBFSTreeRepository,
  MongoDBFileRepository,
  MongoDBFolderRepository
}
import net.scalytica.symbiotic.persistence._
import play.api.{Configuration, Environment}
import repository.mongodb.{AvatarRepository, UserRepository}
import repository.mongodb.party._

object RepositoryTypeNames {
  val MongoRepo = "mongodb"
  val PqsqlRepo = "postgresql"
}

class MongoDBModule(
    environment: Environment,
    configuration: Configuration
) extends AbstractModule {

  def configure() = {
    // val impl: String = configuration.getString("symbiotic.repository.type")
    //   .getOrElse(RepositoryTypeNames.MongoRepo)

    bind(classOf[UserRepository]).to(classOf[MongoDBUserRepository])

    bind(classOf[FolderRepository]).to(classOf[MongoDBFolderRepository])
    bind(classOf[AvatarRepository]).to(classOf[MongoDBAvatarRepository])
    bind(classOf[FileRepository]).to(classOf[MongoDBFileRepository])
    bind(classOf[FSTreeRepository]).to(classOf[MongoDBFSTreeRepository])

  }

}
