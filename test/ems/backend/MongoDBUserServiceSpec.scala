package ems.backend


import com.github.nscala_time.time.Imports._
import play.api.libs.json.JsValue
import securesocial.core.providers.MailToken
import securesocial.core.PasswordInfo
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.WithMongoData
import ems.utils.securesocial.WithSecureSocialUtils
import ems.models.User


@RunWith(classOf[JUnitRunner])
class MongoDBUserServiceSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential

  lazy val service = new MongoDBUserService()

  lazy val json: List[JsValue] = List(user) map {User.userFormat.writes(_)}
  lazy val data = Seq(("users", json))

  "User service" should {
    "Throw exceptions for all username / password related operations" in {
      await(service.saveToken(MailToken("uuid", "email", DateTime.yesterday, DateTime.tomorrow, true))) must throwA[Exception]
      await(service.findToken("token")) must throwA[Exception]
      await(service.deleteToken("uuid")) must throwA[Exception]
      service.deleteExpiredTokens() must throwA[Exception]
      await(service.updatePasswordInfo(user, PasswordInfo("hasher", "password"))) must throwA[Exception]
      await(service.passwordInfoFor(user)) must throwA[Exception]
    }

    "Find basic profile" in new WithMongoData(data) {
      await(service.find(providerId, userId)) should beSome
    }

    "Find user" in new WithMongoData(data) {
      await(service.findUser(providerId, userId)) should beSome
    }

//    "Find basic profile by email" in new WithMongoData(data) {
//      await(service.findByEmailAndProvider(providerId, userEmail)) should beSome
//    }

  }
}