package ems.controllers


import ems.utils.InitDB
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.securesocial.WithSecureSocialUtils
import play.test.WithApplication


@RunWith(classOf[JUnitRunner])
class AuthControllerSpec extends PlaySpecification with WithSecureSocialUtils {
  sequential
//  isolated

  val controller = createController(classOf[AuthController])

  "Auth controller" should {

    "display the account page" in new InitDB() {
      val response = controller.account(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("paul.watson@foobar.com")
    }

  }
}
