package ems.modules

import akka.actor.ActorSystem
import ems.backend.auth.{RedisCookieAuthenticatorStore, RedisAuthenticatorStore, EMSRuntimeEnvironment}
import ems.backend.email.{MailgunService, DefaultMailgunService}
import ems.backend.forwarding.{DefaultForwarderServiceActor, ForwarderServiceActor}
import ems.backend.persistence._
import ems.backend.sms.{DefaultTwilioService, TwilioService}
import ems.backend.updates.{DefaultUpdateService, UpdateService, WebsocketUpdatesServiceActor, UpdatesServiceActor}
import ems.backend.utils.{DefaultRedisService, RedisService, DefaultAkkaServices, AkkaServices}
import ems.controllers.EMSViewTemplates
import ems.controllers.auth.EMSRoutesService
import ems.controllers.utils.HttpsOnlyFilter
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.mvc.Filter
import scaldi.Module
import scaldi.play.condition._
import securesocial.controllers.ViewTemplates
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.{CookieAuthenticatorBuilder, IdGenerator, CookieAuthenticator}
import securesocial.core.services.{CacheService, RoutesService, AuthenticatorService}
import ems.models.User
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.ExecutionContext


class WebModule extends Module {

  bind[Filter] to new HttpsOnlyFilter

  bind[RuntimeEnvironment[User]] to new EMSRuntimeEnvironment
  bind[RoutesService] to new EMSRoutesService()
  // TODO implement a redis based cached service ? This one is based on play's cache
  bind[CacheService] to new CacheService.Default()
  bind[UserStore] to new MongoUserStore
  // use real AuthenticatorService in dev and prod
  bind[AuthenticatorService[User]] to new AuthenticatorService(inject[CookieAuthenticatorBuilder[User]])
  bind[ViewTemplates] to new EMSViewTemplates(inject[RuntimeEnvironment[User]])
  bind[CookieAuthenticatorBuilder[User]] to new CookieAuthenticatorBuilder[User](inject[RedisAuthenticatorStore[CookieAuthenticator[User]]], inject[IdGenerator])
  bind[RedisAuthenticatorStore[CookieAuthenticator[User]]] to new RedisCookieAuthenticatorStore()

  bind[IdGenerator] to new IdGenerator.Default()
  bind[MessageStore] to new MongoMessageStore
  bind[UserInfoStore] to new MongoUserInfoStore
  bind[TwilioService] to new DefaultTwilioService
  bind[MailgunService] to new DefaultMailgunService
  bind[AkkaServices] to new DefaultAkkaServices initWith(_.scheduleAkkaEvents)
  bind[RedisService] to new DefaultRedisService initWith(_.openConnections) destroyWith(_.closeConnections)
  bind[UpdateService] to new DefaultUpdateService

  // get the underlying play akka system (managed by play)
  bind [ActorSystem] to Akka.system
  // get default play execution context
  bind [ExecutionContext] to defaultContext
  // always create new instances of actors
  bind[ForwarderServiceActor] toProvider new DefaultForwarderServiceActor
  bind[UpdatesServiceActor] toProvider new WebsocketUpdatesServiceActor

}
