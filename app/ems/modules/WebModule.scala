package ems.modules

import akka.actor.ActorSystem
import ems.backend.auth.{RedisCookieAuthenticatorStore, RedisAuthenticatorStore, EMSRuntimeEnvironment}
import ems.backend.email.{MailgunService, DefaultMailgunService}
import ems.backend.forwarding.{DefaultForwarderServiceActor, ForwarderServiceActor}
import ems.backend.persistence._
import ems.backend.updates.{UpdateService, WebsocketUpdatesServiceActor, UpdatesServiceActor}
import ems.backend.utils.{DefaultRedisService, RedisService, DefaultAkkaServices, AkkaServices}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scaldi.Module
import scaldi.play.condition._
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.{CookieAuthenticatorBuilder, IdGenerator, CookieAuthenticator}
import securesocial.core.services.{AuthenticatorService}
import ems.models.User


class WebModule extends Module {

  bind[RuntimeEnvironment[User]] to new EMSRuntimeEnvironment
  bind[UserStore] to new MongoUserStore
  // use real AuthenticatorService in dev and prod
  bind[AuthenticatorService[User]] when (inDevMode or inProdMode) to new AuthenticatorService(inject[CookieAuthenticatorBuilder[User]])
  bind[CookieAuthenticatorBuilder[User]] to new CookieAuthenticatorBuilder[User](inject[RedisAuthenticatorStore[CookieAuthenticator[User]]], inject[IdGenerator])
  bind[RedisAuthenticatorStore[CookieAuthenticator[User]]] to new RedisCookieAuthenticatorStore()
  bind[IdGenerator] to new IdGenerator.Default()
  bind[ForwardingStore] to new MongoForwardingStore
  bind[UserInfoStore] to new MongoUserInfoStore
  bind[MailgunService] to new DefaultMailgunService
  bind[AkkaServices] to new DefaultAkkaServices initWith(_.scheduleAkkaEvents)
  bind[RedisService] to new DefaultRedisService initWith(_.openConnections) destroyWith(_.closeConnections)
  bind[UpdateService] to new UpdateService

  // get the underlying play akka system (managed by play)
  bind [ActorSystem] to Akka.system
  // always create new instances of actors
  bind[ForwarderServiceActor] toProvider new DefaultForwarderServiceActor
  bind[UpdatesServiceActor] toProvider new WebsocketUpdatesServiceActor

}
