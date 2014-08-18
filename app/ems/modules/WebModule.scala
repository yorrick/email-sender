package ems.modules

import akka.actor.ActorSystem
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scaldi.Module
import scaldi.play.condition._
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.{CookieAuthenticator, AuthenticatorStore}
import securesocial.core.services.{AuthenticatorService, UserService}

import ems.backend.utils.EMSRuntimeEnvironment
import ems.models.User
import ems.backend.{Forwarder, EMSAuthenticatorService, RedisCookieAuthenticatorStore, UserStore}


class WebModule extends Module {
//  bind identifiedBy "my-message" to "web module message"

  bind[RuntimeEnvironment[User]] to new EMSRuntimeEnvironment

  bind[UserService[User]] to UserStore

  // use real AuthenticatorService in dev and prod
  bind[AuthenticatorService[User]] when (inDevMode or inProdMode) to (new EMSAuthenticatorService)

  bind[AuthenticatorStore[CookieAuthenticator[User]]] to new RedisCookieAuthenticatorStore()

  // get the underlying play akka system (managed by play)
  bind [ActorSystem] to Akka.system

  binding toProvider new Forwarder
}
