package ems.backend

import akka.util.Timeout

import scala.concurrent.duration._

import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.runner._
import akka.util.Timeout._
import play.api.Logger
import play.api.Play._
import play.api.test._
import play.modules.rediscala.RedisPlugin
import securesocial.core.authenticator.{AuthenticatorStore, CookieAuthenticator}

import ems.models.User
import ems.utils.WithRedisData
import ems.utils.securesocial.WithSecureSocialUtils


@RunWith(classOf[JUnitRunner])
class RedisCookieAuthenticationStoreSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential

  implicit lazy val timeout: Timeout = 5.second

  lazy val store = new RedisCookieAuthenticatorStore()

  lazy val authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
    "emailsenderid", user, DateTime.nextDay, DateTime.now, DateTime.lastDay, store)

  lazy val cookieValue = "12345"

  lazy val redisData = Seq(
    (cookieValue, store.byteStringFormatter.serialize(authenticator))
  )

//  def app = FakeApplication(additionalPlugins = Seq("play.modules.rediscala.RedisPlugin"))

  "Authentication store" should {
    "Return Some when authenticator does not exist" in new WithRedisData(redisData) {
      val result = await(store.find(cookieValue))
      result should beSome
    }

    "Return None when authenticator does not exist" in new WithRedisData(redisData) {
      val result = await(store.find("blahblah"))
      result should beNone
    }

//    "Save authenticator" in new WithApplication() {
//
//    }
//
//    "Delete authenticator" in new WithApplication() {
//
//    }

  }
}
