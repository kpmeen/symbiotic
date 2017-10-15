package repository

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import play.api.Configuration
import repository.postgres.party.{
  PostgresAvatarRepository,
  PostgresUserRepository
}
import repository.postgres.silhouette.{
  PostgresOAuth2Repository,
  PostgresPasswordAuthRepository
}

import scala.concurrent.ExecutionContext

class PostgresModule extends AbstractModule {

  def configure(): Unit = {}

  @Provides
  def userRepositoryProvider(configuration: Configuration): UserRepository = {
    new PostgresUserRepository(configuration)
  }

  @Provides
  def avatarRepositoryProvider(
      configuration: Configuration,
      system: ActorSystem,
      materializer: Materializer
  ): AvatarRepository = {
    new PostgresAvatarRepository()(configuration, system, materializer)
  }

  @Provides
  def oauth2RepositoryProvider(
      configuration: Configuration,
      ec: ExecutionContext
  ): DelegableAuthInfoDAO[OAuth2Info] = {
    new PostgresOAuth2Repository(configuration, ec)
  }

  @Provides
  def passAuthRepositoryProvider(
      configuration: Configuration,
      ec: ExecutionContext
  ): DelegableAuthInfoDAO[PasswordInfo] = {
    new PostgresPasswordAuthRepository(configuration, ec)
  }

}
