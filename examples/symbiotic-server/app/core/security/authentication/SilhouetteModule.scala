package core.security.authentication

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{
  Environment,
  EventBus,
  Silhouette,
  SilhouetteProvider
}
import com.mohiva.play.silhouette.crypto._
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.{
  GitHubProvider,
  GoogleProvider
}
import com.mohiva.play.silhouette.impl.providers.state.{
  CsrfStateItemHandler,
  CsrfStateSettings
}
import com.mohiva.play.silhouette.impl.services.GravatarService
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
// scalastyle:off
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
// scalastyle:on
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.party.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class SilhouetteModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[Silhouette[JWTEnvironment]].to[SilhouetteProvider[JWTEnvironment]]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(
      new DefaultFingerprintGenerator(false)
    )
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
      identityServiceImpl = userService,
      authenticatorServiceImpl = authenticatorService,
      requestProvidersImpl = Seq(),
      eventBusImpl = eventBus
    )
  }

  /**
   * Provides the social provider registry.
   *
   * @param googleProvider The Google provider implementation.
   * @param githubProvider The Github provider implementation.
   * @return The Silhouette environment.
   */
  @Provides
  def provideSocialProviderRegistry(
      googleProvider: GoogleProvider,
      githubProvider: GitHubProvider
  ): SocialProviderRegistry = {

    SocialProviderRegistry(
      Seq(
        googleProvider,
        githubProvider
        //      facebookProvider,
        //      twitterProvider,
      )
    )
  }

  /**
   * Provides the signer for the CSRF state item handler.
   *
   * @param configuration The Play configuration.
   * @return The signer for the CSRF state item handler.
   */
  @Provides
  @Named("csrf-state-item-signer")
  def provideCSRFStateItemSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying
      .as[JcaSignerSettings]("silhouette.csrfStateItemHandler.signer")

    new JcaSigner(config)
  }

  /**
   * Provides the cookie signer for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The cookie signer for the authenticator.
   */
  @Provides
  @Named("authenticator-signer")
  def provideAuthenticatorCookieSigner(
      configuration: Configuration
  ): Signer = {
    val config = configuration.underlying
      .as[JcaSignerSettings]("silhouette.authenticator.signer")
    new JcaSigner(config)
  }

  /**
   * Provides the signer for the social state handler.
   *
   * @param configuration The Play configuration.
   * @return The signer for the social state handler.
   */
  @Provides
  @Named("social-state-signer")
  def provideSocialStateSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying
      .as[JcaSignerSettings]("silhouette.socialStateHandler.signer")

    new JcaSigner(config)
  }

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides
  @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying
      .as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  /**
   * Provides the CSRF state item handler.
   *
   * @param idGenerator   The ID generator implementation.
   * @param signer        The signer implementation.
   * @param configuration The Play configuration.
   * @return The CSRF state item implementation.
   */
  @Provides
  def provideCsrfStateItemHandler(
      idGenerator: IDGenerator,
      @Named("csrf-state-item-signer") signer: Signer,
      configuration: Configuration
  ): CsrfStateItemHandler = {
    val settings = configuration.underlying
      .as[CsrfStateSettings]("silhouette.csrfStateItemHandler")
    new CsrfStateItemHandler(settings, idGenerator, signer)
  }

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = {
    new GravatarService(httpLayer)
  }

  /**
   * Provides the password hasher registry.
   *
   * @param hasher The default password hasher implementation.
   * @return The password hasher registry.
   */
  @Provides
  def providePasswordHasherRegistry(
      hasher: PasswordHasher
  ): PasswordHasherRegistry = PasswordHasherRegistry(hasher)

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

    implicit val jwtAuthSettingsReader: ValueReader[JWTAuthenticatorSettings] =
      ValueReader.relative { c =>
        JWTAuthenticatorSettings(
          fieldName = c.as[String]("headerName"),
          issuerClaim = c.as[String]("issuerClaim"),
          // encryptSubject = c.as[Boolean]("encryptSubject"),
          authenticatorExpiry = c.as[FiniteDuration]("authenticatorExpiry"),
          authenticatorIdleTimeout =
            c.getAs[FiniteDuration]("authenticatorIdleTimeout"),
          sharedSecret = c.as[String]("sharedSecret")
        )
      }
    val config = configuration.underlying
      .as[JWTAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)
    new JWTAuthenticatorService(config, None, encoder, idGenerator, clock)
  }

  // scalastyle:off line.length
  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the password auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(
      passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
      oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info]
  ): AuthInfoRepository = {
    new DelegableAuthInfoRepository(passwordInfoDAO, oauth2InfoDAO)
  }

  // scalastyle:on line.length

  /**
   * Provides the social state handler.
   *
   * @param signer The signer implementation.
   * @return The social state handler implementation.
   */
  @Provides
  def provideSocialStateHandler(
      @Named("social-state-signer") signer: Signer,
      csrfStateItemHandler: CsrfStateItemHandler
  ): SocialStateHandler = {
    new DefaultSocialStateHandler(Set(csrfStateItemHandler), signer)
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
  ): CredentialsProvider = {
    new CredentialsProvider(authInfoRepository, passwordHasher)
  }

  /**
   * Provides the Google provider.
   *
   * @param httpLayer          The HTTP layer implementation.
   * @param socialStateHandler The OAuth2 state provider implementation.
   * @param configuration      The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
      httpLayer: HTTPLayer,
      socialStateHandler: SocialStateHandler,
      configuration: Configuration
  ): GoogleProvider = {
    new GoogleProvider(
      httpLayer,
      socialStateHandler,
      configuration.underlying.as[OAuth2Settings]("silhouette.google")
    )
  }

  @Provides
  def provideGithubProvider(
      httpLayer: HTTPLayer,
      socialStateHandler: SocialStateHandler,
      configuration: Configuration
  ): GitHubProvider = {
    new GitHubProvider(
      httpLayer,
      socialStateHandler,
      configuration.underlying.as[OAuth2Settings]("silhouette.github")
    )
  }

}
