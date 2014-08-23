package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.WithTestData
import ems.utils.securesocial.WithSecureSocialUtils


@RunWith(classOf[JUnitRunner])
class ApplicationControllerSpec extends PlaySpecification with WithSecureSocialUtils with WithTestData {

  sequential

  "Main module" should {

    "send 404 on a bad request" in new WithApplication(app) {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication(app) {
      val controller = app.global.getControllerInstance(classOf[Application])
      val home = controller.index(FakeRequest().withCookies(cookie))

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Welcome")
      contentAsString(home) must contain("Paul Watson")
    }
  }

}
