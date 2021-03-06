package core.security.authentication

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.party.User

trait JWTEnvironment extends Env {

  type I = User
  type A = JWTAuthenticator

}
