/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package core.security.authentication

import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.GoogleProvider
import com.mohiva.play.silhouette.impl.providers.oauth2.state.{CookieStateProvider, CookieStateSettings}
import com.mohiva.play.silhouette.impl.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.util._
import models.party.User
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import repository.mongodb.silhouette.{MongoDBOAuth2Repository, MongoDBPasswordAuthRepository}
import services.party.UserService

class SilhouetteModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[MongoDBPasswordAuthRepository]
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[MongoDBOAuth2Repository]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus
  ): Environment[User, JWTAuthenticator] = {

    Environment[User, JWTAuthenticator](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  /**
   * Provides the authenticator service.
   *
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock
  ): AuthenticatorService[JWTAuthenticator] = {

    val config = configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.authenticator")
    new JWTAuthenticatorService(config, None, idGenerator, clock)
  }

  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the password auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(
    passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
    //    oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
    oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info]
  //    openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]
  ): AuthInfoRepository =
    new DelegableAuthInfoRepository(passwordInfoDAO, oauth2InfoDAO) //, oauth1InfoDAO, openIDInfoDAO)

  /**
   * Provides the OAuth2 state provider.
   *
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The OAuth2 state provider implementation.
   */
  @Provides
  def provideOAuth2StateProvider(idGenerator: IDGenerator, configuration: Configuration, clock: Clock): OAuth2StateProvider = {
    val settings = configuration.underlying.as[CookieStateSettings]("silhouette.oauth2StateProvider")
    new CookieStateProvider(settings, idGenerator, clock)
  }

  /**
   * Provides the social provider registry.
   *
   * //   * @param facebookProvider The Facebook provider implementation.
   *
   * @param googleProvider The Google provider implementation.
   * //   * @param twitterProvider The Twitter provider implementation.
   * //   * @param yahooProvider The Yahoo provider implementation.
   * @return The Silhouette environment.
   */
  @Provides
  def provideSocialProviderRegistry(
    //    facebookProvider: FacebookProvider,
    googleProvider: GoogleProvider
  //    twitterProvider: TwitterProvider,
  //    yahooProvider: YahooProvider
  ): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(
      googleProvider
    //      facebookProvider,
    //      twitterProvider,
    //      yahooProvider
    ))
  }

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasher The default password hasher implementation.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasher: PasswordHasher
  ): CredentialsProvider =
    new CredentialsProvider(
      authInfoRepository, passwordHasher, Seq(passwordHasher)
    )

  /**
   * Provides the Google provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @param configuration The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    configuration: Configuration
  ): GoogleProvider =
    new GoogleProvider(
      httpLayer, stateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.google")
    )

}
