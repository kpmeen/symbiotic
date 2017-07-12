package repository

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import repository.mongodb.{AvatarRepository, UserRepository}
import repository.mongodb.party._

class MongoDBModule(
    environment: Environment,
    configuration: Configuration
) extends AbstractModule {

  def configure() = {
    bind(classOf[UserRepository]).to(classOf[MongoDBUserRepository])
    bind(classOf[AvatarRepository]).to(classOf[MongoDBAvatarRepository])
  }

}
