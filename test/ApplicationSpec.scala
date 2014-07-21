import ems.models.{SmsDisplay, Sms, SavedInMongo}

import com.github.nscala_time.time.Imports.DateTime

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner._

import play.api.libs.json.{JsValue}
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._

import reactivemongo.bson.BSONObjectID


@RunWith(classOf[JUnitRunner])
class SmsSpec extends Specification {
  sequential
//  isolated

  step {
    Logger.info("Before class")
  }

  val smsList = List(Sms(BSONObjectID.generate, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  val bsonList: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  val data = ("smslist", bsonList)

  "Sms module" should {

    "render the sms list page" in new InitDB(data) {
      val response = ems.controllers.SmsController.list()(FakeRequest())

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("some text")
    }

    "Accept post data for sms" in new InitDB(data) {
      val request = FakeRequest(POST, "/sms/").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val postResponse = ems.controllers.TwilioController.sms(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must contain("Message")

      val listResponse = ems.controllers.SmsController.list()(FakeRequest())
      status(listResponse) must equalTo(OK)
      contentType(listResponse) must beSome.which(_ == "text/html")
      contentAsString(listResponse) must contain ("some text")
      contentAsString(listResponse) must contain ("hello toto")
    }

  }

  "Main module" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val home = ems.controllers.Application.index()(FakeRequest())

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Send emails")
    }
  }

  step {
    Logger.info("After class")
  }
}
