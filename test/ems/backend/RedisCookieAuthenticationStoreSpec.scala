package ems.backend


import scala.concurrent.duration._

import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.runner._
import akka.util.Timeout._
import akka.util.Timeout
import securesocial.core.authenticator.CookieAuthenticator
import play.api.test._

import ems.models.User
import ems.utils.WithRedisData
import ems.utils.securesocial.WithSecureSocialUtils


@RunWith(classOf[JUnitRunner])
class RedisCookieAuthenticationStoreSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential

  implicit lazy val timeout: Timeout = 5.second

  lazy val store = new RedisCookieAuthenticatorStore()

  lazy val authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
    cookieValue, user, DateTime.nextDay, DateTime.now, DateTime.lastDay, store)

  lazy val cookieValue = "autenticatorId"

  lazy val redisData = Seq(
    (cookieValue, store.byteStringFormatter.serialize(authenticator))
  )

  "Authentication store" should {
    "Return Some when authenticator does exist" in new WithRedisData(redisData) {
      val result = await(store.find(cookieValue))
      result should beSome
    }

    "Return None when authenticator does not exist" in new WithRedisData(redisData) {
      val result = await(store.find("blahblah"))
      result should beNone
    }

    "Save authenticator" in new WithApplication() {
      val newId = "67890"
      val result = await(store.save(authenticator.copy(id = newId), 1))
      result.id must beEqualTo(newId)
    }

    "Delete authenticator" in new WithApplication() {
      await(store.delete(cookieValue))
    }

  }
}
