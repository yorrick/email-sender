package ems.backend


import ems.models.User
import scaldi.Injectable
import scaldi.play.ScaldiSupport
import securesocial.core.authenticator.{AuthenticatorStore, CookieAuthenticator}

import scala.concurrent.duration._

import org.junit.runner.RunWith
import org.specs2.runner._
import akka.util.Timeout._
import akka.util.Timeout
import play.api.test._

import ems.utils.{WithMongoTestData, WithRedisTestData, WithRedisData}
import ems.utils.securesocial.WithSecureSocialUtils


@RunWith(classOf[JUnitRunner])
class RedisCookieAuthenticationStoreSpec extends PlaySpecification with WithSecureSocialUtils
      with WithRedisTestData with WithMongoTestData with Injectable {

  sequential

  implicit lazy val timeout: Timeout = 5.second

  "Authentication store" should {
    "Return Some when authenticator does exist" in new WithRedisData(redisData) {
      implicit val injector = app.global.asInstanceOf[ScaldiSupport].injector
      val authStore = inject[AuthenticatorStore[CookieAuthenticator[User]]]

      val result = await(authStore.find(cookieValue))
      result should beSome
    }

    "Return None when authenticator does not exist" in new WithRedisData(redisData) {
      implicit val injector = app.global.asInstanceOf[ScaldiSupport].injector
      val authStore = inject[AuthenticatorStore[CookieAuthenticator[User]]]

      val result = await(authStore.find("blahblah"))
      result should beNone
    }

    "Save authenticator" in new WithRedisData(redisData) {
      implicit val injector = app.global.asInstanceOf[ScaldiSupport].injector
      val authStore = inject[AuthenticatorStore[CookieAuthenticator[User]]]

      val newId = "67890"
      val result = await(authStore.save(authenticator.copy(id = newId), 1))
      result.id must beEqualTo(newId)
    }

    "Delete authenticator" in new WithRedisData(redisData) {
      implicit val injector = app.global.asInstanceOf[ScaldiSupport].injector
      val authStore = inject[AuthenticatorStore[CookieAuthenticator[User]]]

      await(authStore.delete(cookieValue))
    }

  }
}
