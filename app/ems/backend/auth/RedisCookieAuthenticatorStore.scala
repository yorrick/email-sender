package ems.backend.auth

import ems.backend.utils.RedisService
import ems.models.User
import scaldi.{Injectable, Injector}
import securesocial.core.authenticator.CookieAuthenticator

import scala.concurrent.ExecutionContext


/**
 * Implementation for CookieAuthenticator
 */
class RedisCookieAuthenticatorStore(implicit inj: Injector)
    extends RedisAuthenticatorStore[CookieAuthenticator[User]] with Injectable {

  override val byteStringFormatter = new CookieAuthenticatorFormatter(this)
  implicit val executionContext = inject[ExecutionContext]

  val redisClient = inject[RedisService].client
}
