package ems.backend


import com.github.nscala_time.time.Imports._
import play.api.libs.json.JsValue
import securesocial.core.providers.MailToken
import securesocial.core.PasswordInfo
import securesocial.core.services.SaveMode
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoTestData, WithMongoApplication}
import ems.utils.securesocial.WithSecureSocialUtils
import ems.models.User


@RunWith(classOf[JUnitRunner])
class UserStoreSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential

  lazy val service = UserStore

  "User store" should {
    "Throw exceptions for all username / password related operations" in {
      await(service.saveToken(MailToken("uuid", "email", DateTime.yesterday, DateTime.tomorrow, true))) must throwA[Exception]
      await(service.findToken("token")) must throwA[Exception]
      await(service.deleteToken("uuid")) must throwA[Exception]
      service.deleteExpiredTokens() must throwA[Exception]
      await(service.updatePasswordInfo(user, PasswordInfo("hasher", "password"))) must throwA[Exception]
      await(service.passwordInfoFor(user)) must throwA[Exception]
      await(service.passwordInfoFor(user)) must throwA[Exception]
      await(service.link(user, profile)) must throwA[Exception]
    }

    "Find basic profile" in new WithMongoApplication(data) {
      await(service.find(providerId, userId)) should beSome
    }

    "Find user" in new WithMongoApplication(data) {
      await(service.findUser(providerId, userId)) should beSome
    }

    "Find user by id" in new WithMongoApplication(data) {
      await(service.findUserById(userMongoId)).id must beEqualTo(userMongoId)
    }

    "Find basic profile by email" in new WithMongoApplication(data) {
      await(service.findByEmailAndProvider(userEmail, providerId)) should beSome
    }

    "Save a profile" in new WithMongoApplication(data) {
      await(service.save(profile, SaveMode.LoggedIn)).main.userId must beEqualTo(userId)
    }

  }
}
