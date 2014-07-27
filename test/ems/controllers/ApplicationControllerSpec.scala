package ems.controllers


import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.runner._
import reactivemongo.bson.BSONObjectID
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Cookie
import play.api.test._

import ems.models.{SavedInMongo, Sms}
import ems.utils.securesocial.WithSecureSocialUtils


@RunWith(classOf[JUnitRunner])
class ApplicationControllerSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential
//  isolated

  val applicationController = createController(classOf[Application])

  step {
    Logger.info("Before class")
  }

  val smsId = "53cd93ce93d970b47bea76fd"
  val smsList = List(Sms(BSONObjectID.parse(smsId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  val bsonList: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  val data = ("smslist", bsonList)


  "Main module" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val home = applicationController.index(FakeRequest().withCookies(cookie))

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Sms forwardings")
    }
  }

  step {
    Logger.info("After class")
  }
}
