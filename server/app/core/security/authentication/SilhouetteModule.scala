/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package core.security.authentication

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.crypto.{JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.{CookieStateProvider, CookieStateSettings}
import com.mohiva.play.silhouette.impl.providers.oauth2.{GitHubProvider, GoogleProvider}
import com.mohiva.play.silhouette.impl.services.GravatarService
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import repository.mongodb.silhouette.{MongoDBOAuth2Repository, MongoDBPasswordAuthRepository}
import services.party.UserService

import scala.concurrent.duration.FiniteDuration

class SilhouetteModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {
    bind[Silhouette[JWTEnvironment]].to[SilhouetteProvider[JWTEnvironment]]
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
   * @param userService          The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus             The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus
  ): Environment[JWTEnvironment] = {

    Environment[JWTEnvironment](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
   * Provides the cookie signer for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The cookie signer for the authenticator.
   */
  @Provides @Named("authenticator-cookie-signer")
  def provideAuthenticatorCookieSigner(configuration: Configuration): CookieSigner = {
    val config = configuration.underlying.as[JcaCookieSignerSettings]("silhouette.authenticator.cookie.signer")
    new JcaCookieSigner(config)
  }

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  /**
   * Provides the cookie signer for the OAuth2 state provider.
   *
   * @param configuration The Play configuration.
   * @return The cookie signer for the OAuth2 state provider.
   */
  @Provides @Named("oauth2-state-cookie-signer")
  def provideOAuth2StageCookieSigner(configuration: Configuration): CookieSigner = {
    val config = configuration.underlying.as[JcaCookieSignerSettings]("silhouette.oauth2StateProvider.cookie.signer")
    new JcaCookieSigner(config)
  }

  /**
   * Provides the password hasher registry.
   *
   * @param passwordHasher The default password hasher implementation.
   * @return The password hasher registry.
   */
  @Provides
  def providePasswordHasherRegistry(passwordHasher: PasswordHasher): PasswordHasherRegistry = {
    PasswordHasherRegistry(passwordHasher)
  }

  /**
   * Provides the authenticator service.
   *
   * @param idGenerator   The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock         The clock instance.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-crypter") crypter: Crypter,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock
  ): AuthenticatorService[JWTAuthenticator] = {

    implicit val jwtAuthSettingsReader: ValueReader[JWTAuthenticatorSettings] = ValueReader.relative(c =>
      JWTAuthenticatorSettings(
        fieldName = c.as[String]("headerName"),
        issuerClaim = c.as[String]("issuerClaim"),
        //        encryptSubject = c.as[Boolean]("encryptSubject"),
        authenticatorExpiry = c.as[FiniteDuration]("authenticatorExpiry"),
        authenticatorIdleTimeout = c.getAs[FiniteDuration]("authenticatorIdleTimeout"),
        sharedSecret = c.as[String]("sharedSecret")
      ))
    val config = configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)
    new JWTAuthenticatorService(config, None, encoder, idGenerator, clock)
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
   * @param idGenerator   The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock         The clock instance.
   * @return The OAuth2 state provider implementation.
   */
  @Provides
  def provideOAuth2StateProvider(
    idGenerator: IDGenerator,
    @Named("oauth2-state-cookie-signer") cookieSigner: CookieSigner,
    configuration: Configuration,
    clock: Clock
  ): OAuth2StateProvider = {
    val settings = configuration.underlying.as[CookieStateSettings]("silhouette.oauth2StateProvider")
    new CookieStateProvider(settings, idGenerator, cookieSigner, clock)
  }

  /**
   * Provides the social provider registry.
   *
   * //   * @param facebookProvider The Facebook provider implementation.
   *
   * @param googleProvider The Google provider implementation.
   *                       //   * @param twitterProvider The Twitter provider implementation.
   *                       //   * @param yahooProvider The Yahoo provider implementation.
   * @return The Silhouette environment.
   */
  @Provides
  def provideSocialProviderRegistry(
    googleProvider: GoogleProvider,
    githubProvider: GitHubProvider
  //    facebookProvider: FacebookProvider,
  //    twitterProvider: TwitterProvider,
  //    yahooProvider: YahooProvider
  ): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(
      googleProvider,
      githubProvider
    //      facebookProvider,
    //      twitterProvider,
    //      yahooProvider
    ))
  }

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasher     The default password hasher implementation.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasher: PasswordHasherRegistry
  ): CredentialsProvider =
    new CredentialsProvider(authInfoRepository, passwordHasher)

  /**
   * Provides the Google provider.
   *
   * @param httpLayer     The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @param configuration The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    configuration: Configuration
  ): GoogleProvider = {
    new GoogleProvider(
      httpLayer,
      stateProvider,
      configuration.underlying.as[OAuth2Settings]("silhouette.google")
    )
  }

  @Provides
  def provideGithubProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    configuration: Configuration
  ): GitHubProvider = {
    new GitHubProvider(
      httpLayer,
      stateProvider,
      configuration.underlying.as[OAuth2Settings]("silhouette.github")
    )
  }

}
