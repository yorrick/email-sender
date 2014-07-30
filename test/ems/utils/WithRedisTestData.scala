package ems.utils

import _root_.securesocial.core.authenticator.CookieAuthenticator
import com.github.nscala_time.time.Imports._
import ems.backend.RedisCookieAuthenticatorStore
import ems.models.User
import ems.utils.securesocial.WithSecureSocialUtils

/**
 * Provides data for redis based tests
 */
trait WithRedisTestData { self: WithMongoTestData =>

  lazy val store = new RedisCookieAuthenticatorStore()

  lazy val authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
    cookieValue, user, DateTime.nextDay, DateTime.now, DateTime.lastDay, store)

  lazy val cookieValue = "autenticatorId"

  lazy val redisData = Seq(
    (cookieValue, store.byteStringFormatter.serialize(authenticator))
  )
}
