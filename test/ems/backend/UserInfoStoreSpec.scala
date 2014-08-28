package ems.backend

import ems.backend.persistence.mongo.MongoDBUtils
import ems.backend.persistence.{MessageStore, UserInfoStore}
import ems.utils.{TestUtils, AppInjector, WithTestData, WithMongoApplication}
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import scaldi.Injectable


@RunWith(classOf[JUnitRunner])
class UserInfoStoreSpec extends PlaySpecification with MongoDBUtils with WithTestData with AppInjector with Injectable with TestUtils {
  sequential

  "User info store" should {

    "Find by userId" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      await(store.findUserInfoByUserId(userMongoId)).id must beEqualTo(userMongoId)
    }

    "Find by phone number" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      await(store.findUserInfoByPhoneNumber(phoneNumber)).id must beEqualTo(userMongoId)
    }

    "Create user info" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      val userInfoToCreate = userInfo.copy(_id = generateId)
      await(store.createUserInfo(userInfoToCreate)).id must beEqualTo(userInfoToCreate.id)
    }

    "Save phone number" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      await(store.savePhoneNumber(userMongoId, phoneNumber)).id must beEqualTo(userMongoId)
    }

  }
}
