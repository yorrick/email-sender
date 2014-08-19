package ems.modules

import akka.actor.ActorSystem
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scaldi.Module
import scaldi.play.condition._
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.{CookieAuthenticatorBuilder, IdGenerator, CookieAuthenticator, AuthenticatorStore}
import securesocial.core.services.{AuthenticatorService, UserService}

import ems.backend.utils.{RedisCookieAuthenticatorStore, EMSRuntimeEnvironment}
import ems.models.User
import ems.backend._


class WebModule extends Module {

  bind[RuntimeEnvironment[User]] to new EMSRuntimeEnvironment
  bind[UserService[User]] to UserStore
  // use real AuthenticatorService in dev and prod
  bind[AuthenticatorService[User]] when (inDevMode or inProdMode) to new AuthenticatorService(inject[CookieAuthenticatorBuilder[User]])
  bind[CookieAuthenticatorBuilder[User]] to new CookieAuthenticatorBuilder[User](inject[AuthenticatorStore[CookieAuthenticator[User]]], inject[IdGenerator])
  bind[AuthenticatorStore[CookieAuthenticator[User]]] to new RedisCookieAuthenticatorStore()
  bind[IdGenerator] to new IdGenerator.Default()
  bind[ForwardingStore] to new MongoForwardingStore
  bind[MailgunService] to new DefaultMailgunService

  // get the underlying play akka system (managed by play)
  bind [ActorSystem] to Akka.system
  // always create new instances of actors
  bind[ForwarderService] toProvider new Forwarder

}
