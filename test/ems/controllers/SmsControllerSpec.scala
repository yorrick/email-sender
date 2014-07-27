package ems.controllers


import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.runner._
import reactivemongo.bson.BSONObjectID
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.test._

import ems.models.{SavedInMongo, Sms}
import ems.utils.securesocial.WithSecureSocialUtils
import ems.utils.InitDB


@RunWith(classOf[JUnitRunner])
class SmsControllerSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential
//  isolated

  val smsController = createController(classOf[SmsController])

  step {
    Logger.info("Before class")
  }

  val smsId = "53cd93ce93d970b47bea76fd"
  val smsList = List(Sms(BSONObjectID.parse(smsId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  val bsonList: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  val data = ("smslist", bsonList)

  "Sms controller" should {

    "render the sms list page" in new InitDB(data) {
      val response = smsController.list(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("some text")
    }

    "block access to non logged users" in new InitDB(data) {
      val response = smsController.list(FakeRequest())

      status(response) must equalTo(UNAUTHORIZED)
    }

  }

  step {
    Logger.info("After class")
  }
}
