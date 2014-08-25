package ems.controllers


import _root_.securesocial.core.RuntimeEnvironment
import ems.backend.email.MailgunService
import ems.backend.persistence.{UserStore, UserInfoStore}
import ems.backend.sms.TwilioService
import ems.models.User
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import ems.utils._
import scaldi.{Injectable, Module}
import scaldi.play.ControllerInjector
import play.api.Application

import scala.concurrent.ExecutionContext


@RunWith(classOf[JUnitRunner])
class AccountControllerSpec extends PlaySpecification with TestUtils with WithTestData with Injectable {
  sequential

  implicit val injector = new Module {
    bind[Application] to app

    bind[RuntimeEnvironment[User]] to mockRuntimeEnvironment
    bind[UserStore] to mockUserStore
    bind[ExecutionContext] to mockExecutionContext
    bind[MailgunService] to mockMailgunService
    bind[UserInfoStore] to mockUserInfoStore
    bind[TwilioService] to mockTwilioService
  } :: new ControllerInjector

  "Account controller" should {

    "display the account page" in {
      val response = inject[AccountController].account(FakeRequest().withCookies(cookie))
      status(response) must equalTo(OK)

      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("paul.watson@foobar.com")
    }

    "Update phone number" in {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> "0123456789"
      )

      val response = inject[AccountController].accountUpdate(request.withCookies(cookie))

      status(response) must equalTo(SEE_OTHER)
      flash(response).isEmpty must beFalse
      redirectLocation(response) must beEqualTo(Some(ems.controllers.routes.AccountController.account.url))
    }

    "Block wrong phone number" in {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> "0123"
      )

      val response = inject[AccountController].accountUpdate(request.withCookies(cookie))
      status(response) must equalTo(BAD_REQUEST)
    }

    "Block duplicate phone number" in {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "phoneNumber" -> phoneNumber
      )

      val response = inject[AccountController].accountUpdate(request.withCookies(cookie))
      status(response) must equalTo(BAD_REQUEST)
    }

  }

}
