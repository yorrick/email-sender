package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.securesocial.WithSecureSocialUtils


@RunWith(classOf[JUnitRunner])
class AccountControllerSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential

  val controller = createController(classOf[AccountController])

  "Account controller" should {

    "display the account page" in new WithApplication() {
      val response = controller.account(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("paul.watson@foobar.com")
    }

    "Update phone number" in new WithApplication() {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> "0123456789"
      )

      val response = controller.accountUpdate(request.withCookies(cookie))

      status(response) must equalTo(SEE_OTHER)
      flash(response).isEmpty must beFalse
      redirectLocation(response) must beEqualTo(Some(ems.controllers.routes.AccountController.account.url))
    }

    "Block wrong phone number" in new WithApplication() {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> "0123"
      )

      val response = controller.accountUpdate(request.withCookies(cookie))
      status(response) must equalTo(BAD_REQUEST)
    }

  }
}
