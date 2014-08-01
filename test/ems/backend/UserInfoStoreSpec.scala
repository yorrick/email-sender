package ems.backend

import ems.utils.WithMongoData
import ems.utils.securesocial.WithSecureSocialUtils
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._


@RunWith(classOf[JUnitRunner])
class UserInfoStoreSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential

  "User info store" should {

    "Find by userId" in new WithMongoData(data) {
      await(UserInfoStore.findUserInfoByUserId(userMongoId)).id must beEqualTo(userMongoId)
    }

    "Find by phone number" in new WithMongoData(data) {
      await(UserInfoStore.findUserInfoByPhoneNumber(phoneNumber)).id must beEqualTo(userMongoId)
    }

    "Create user info" in new WithMongoData(data) {
      val userInfoToCreate = userInfo.copy(_id = UserInfoStore.generateId)
      await(UserInfoStore.createUserInfo(userInfoToCreate)).id must beEqualTo(userInfoToCreate.id)
    }

    "Save phone number" in new WithMongoData(data) {
      await(UserInfoStore.savePhoneNumber(userMongoId, phoneNumber)).id must beEqualTo(userMongoId)
    }

  }
}
