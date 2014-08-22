package ems.backend

import ems.backend.persistence.mongo.MongoDBUtils
import ems.backend.persistence.{ForwardingStore, UserInfoStore}
import ems.utils.{AppInjector, WithMongoTestData, WithMongoApplication}
import ems.utils.securesocial.WithSecureSocialUtils
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import scaldi.Injectable


@RunWith(classOf[JUnitRunner])
class UserInfoStoreSpec extends PlaySpecification with WithSecureSocialUtils with MongoDBUtils with WithMongoTestData with AppInjector with Injectable {
  sequential

  "User info store" should {

    "Find by userId" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      await(store.findUserInfoByUserId(userMongoId)).id must beEqualTo(userMongoId)
    }

    "Find by phone number" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      await(store.findUserInfoByPhoneNumber(phoneNumber)).id must beEqualTo(userMongoId)
    }

    "Create user info" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      val userInfoToCreate = userInfo.copy(_id = generateId)
      await(store.createUserInfo(userInfoToCreate)).id must beEqualTo(userInfoToCreate.id)
    }

    "Save phone number" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[UserInfoStore]

      await(store.savePhoneNumber(userMongoId, phoneNumber)).id must beEqualTo(userMongoId)
    }

  }
}
