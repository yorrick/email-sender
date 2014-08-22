package ems.utils

import _root_.securesocial.core.authenticator.{AuthenticatorStore, CookieAuthenticator}
import com.github.nscala_time.time.Imports._
import ems.backend.utils.{RedisAuthenticatorStore, RedisCookieAuthenticatorStore}
import ems.models.User
import ems.modules.WebModule
import ems.utils.securesocial.WithSecureSocialUtils
import scaldi.{Injectable, Injector}

/**
 * Provides data for redis based tests
 */
trait WithRedisTestData extends WithMongoTestData with Injectable {

  implicit val injector: Injector = new WebModule

  lazy val cookieValue = "autenticatorId"

//  def store = new RedisCookieAuthenticatorStore(injector)
  def store = inject[RedisAuthenticatorStore[CookieAuthenticator[User]]]

  def authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
    cookieValue, user, DateTime.nextDay, DateTime.now, DateTime.lastDay, store)

  def redisData = Seq(
    (cookieValue, store.byteStringFormatter.serialize(authenticator))
  )
}
