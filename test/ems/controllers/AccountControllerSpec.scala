package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.securesocial.WithSecureSocialUtils
import ems.utils.{WithMongoTestData, WithMongoApplication}


@RunWith(classOf[JUnitRunner])
class AccountControllerSpec extends PlaySpecification with WithSecureSocialUtils with WithMongoTestData {
  sequential

  "Account controller" should {

    "display the account page" in new WithMongoApplication(data, app) {
      val response = app.global.getControllerInstance(classOf[AccountController]).account(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("paul.watson@foobar.com")
    }

    "Update phone number" in new WithMongoApplication(data, app) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> "0123456789"
      )

      val response = app.global.getControllerInstance(classOf[AccountController]).accountUpdate(request.withCookies(cookie))

      status(response) must equalTo(SEE_OTHER)
      flash(response).isEmpty must beFalse
      redirectLocation(response) must beEqualTo(Some(ems.controllers.routes.AccountController.account.url))
    }

    "Block wrong phone number" in new WithApplication(app) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> "0123"
      )

      val response = app.global.getControllerInstance(classOf[AccountController]).accountUpdate(request.withCookies(cookie))
      status(response) must equalTo(BAD_REQUEST)
    }

    "Block duplicate phone number" in new WithApplication(app) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> phoneNumber
      )

      val response = app.global.getControllerInstance(classOf[AccountController]).accountUpdate(request.withCookies(cookie))
      status(response) must equalTo(BAD_REQUEST)
    }

  }

}
