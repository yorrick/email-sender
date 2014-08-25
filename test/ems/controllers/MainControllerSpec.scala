package ems.controllers


import ems.models.User
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.Application
import play.api.test._

import ems.utils.{MockUtils, WithTestData}
import scaldi.{Injectable, Module}
import scaldi.play.ControllerInjector
import securesocial.core.RuntimeEnvironment


@RunWith(classOf[JUnitRunner])
class MainControllerSpec extends PlaySpecification with MockUtils with WithTestData with Injectable {
  sequential

  implicit val injector = new Module {
    bind[Application] to app
    bind[RuntimeEnvironment[User]] to mockRuntimeEnvironment
  } :: new ControllerInjector

  "Main module" should {

    "send 404 on a bad request" in new WithApplication(app) {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in {
      val home = inject[MainController].index(FakeRequest().withCookies(cookie))

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Welcome")
      contentAsString(home) must contain("Paul Watson")
    }
  }

}
