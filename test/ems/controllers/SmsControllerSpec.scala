package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.http.{MimeTypes, HeaderNames}
import play.api.Logger
import play.api.test._

import ems.utils.securesocial.WithSecureSocialUtils
import ems.utils.{WithMongoTestData, WithMongoData}


@RunWith(classOf[JUnitRunner])
class SmsControllerSpec extends PlaySpecification with WithSecureSocialUtils with WithMongoTestData {
  sequential
//  isolated

  val smsController = createController(classOf[SmsController])

  step {
    Logger.info("Before class")
  }

  "Sms controller" should {

    "render the sms list page" in new WithMongoData(data) {
      val response = smsController.list(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("some text")
    }

    "block access to non logged users" in new WithMongoData(data) {
      val response = smsController.list(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.HTML))

      status(response) must equalTo(SEE_OTHER)
    }

  }

  step {
    Logger.info("After class")
  }
}
