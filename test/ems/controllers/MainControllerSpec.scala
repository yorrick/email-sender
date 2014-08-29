package ems.controllers


import ems.models.User
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{AppInjector, TestUtils, WithTestData}
import scaldi.{Injectable, Module}
import securesocial.core.RuntimeEnvironment


@RunWith(classOf[JUnitRunner])
class MainControllerSpec extends PlaySpecification with TestUtils with WithTestData with AppInjector with Injectable {
  sequential

  def testInjector = new Module {
    bind[RuntimeEnvironment[User]] to mockRuntimeEnvironment
  }

  "Main module" should {

    "send 404 on a bad request" in new WithApplication(app) {
      implicit val i = testInjector :: appInjector
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication(app) {
      implicit val i = testInjector :: appInjector
      val home = inject[MainController].index(FakeRequest().withCookies(cookie))

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Welcome")
      contentAsString(home) must contain("Paul Watson")
    }
  }

}
