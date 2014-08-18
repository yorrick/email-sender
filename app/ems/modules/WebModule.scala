package ems.modules

import akka.actor.ActorSystem
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scaldi.Module
import scaldi.play.condition._
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.{IdGenerator, CookieAuthenticator, AuthenticatorStore}
import securesocial.core.services.{AuthenticatorService, UserService}

import ems.backend.utils.{EMSCookieAuthenticatorBuilder, RedisCookieAuthenticatorStore, EMSAuthenticatorService, EMSRuntimeEnvironment}
import ems.models.User
import ems.backend.{Forwarder, UserStore}


class WebModule extends Module {

  bind[RuntimeEnvironment[User]] to new EMSRuntimeEnvironment

  bind[UserService[User]] to UserStore

  // use real AuthenticatorService in dev and prod
  bind[AuthenticatorService[User]] when (inDevMode or inProdMode) to (new EMSAuthenticatorService)

  bind[EMSCookieAuthenticatorBuilder] to new EMSCookieAuthenticatorBuilder

  bind[AuthenticatorStore[CookieAuthenticator[User]]] to new RedisCookieAuthenticatorStore()

  bind[IdGenerator] to new IdGenerator.Default()

  // get the underlying play akka system (managed by play)
  bind [ActorSystem] to Akka.system

  binding toProvider new Forwarder
}
