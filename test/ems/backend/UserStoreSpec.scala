package ems.backend


import com.github.nscala_time.time.Imports._
import ems.backend.persistence.{UserInfoStore, UserStore}
import play.api.libs.json.JsValue
import scaldi.Injectable
import securesocial.core.providers.MailToken
import securesocial.core.PasswordInfo
import securesocial.core.services.SaveMode
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{TestUtils, AppInjector, WithTestData, WithMongoApplication}
import ems.models.User


@RunWith(classOf[JUnitRunner])
class UserStoreSpec extends PlaySpecification with WithTestData with AppInjector with Injectable with TestUtils {
  sequential

  "User store" should {
    "Throw exceptions for all username / password related operations" in new WithApplication(app = noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserStore]

      await(store.saveToken(MailToken("uuid", "email", DateTime.yesterday, DateTime.tomorrow, true))) must throwA[Exception]
      await(store.findToken("token")) must throwA[Exception]
      await(store.deleteToken("uuid")) must throwA[Exception]
      store.deleteExpiredTokens() must throwA[Exception]
      await(store.updatePasswordInfo(user, PasswordInfo("hasher", "password"))) must throwA[Exception]
      await(store.passwordInfoFor(user)) must throwA[Exception]
      await(store.passwordInfoFor(user)) must throwA[Exception]
      await(store.link(user, profile)) must throwA[Exception]
    }

    "Find basic profile" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserStore]

      await(store.find(providerId, userId)) should beSome
    }

    "Find user" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserStore]

      await(store.findUser(userId)) should beSome
    }

    "Find user by id" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserStore]

      await(store.findUserById(userMongoId)).id must beEqualTo(userMongoId)
    }

    "Find basic profile by email" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserStore]

      await(store.findByEmailAndProvider(userEmail, providerId)) should beSome
    }

    "Save a profile" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserStore]

      await(store.save(profile, SaveMode.LoggedIn)).main.userId must beEqualTo(userId)
    }

  }
}
