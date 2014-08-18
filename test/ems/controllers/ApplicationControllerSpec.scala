package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import scaldi.Injectable

import ems.utils.WithMongoTestData
import ems.utils.securesocial.WithSecureSocialUtils
import ems.backend.Global


@RunWith(classOf[JUnitRunner])
class ApplicationControllerSpec extends PlaySpecification with WithSecureSocialUtils
      with WithMongoTestData with Injectable {

  sequential

//  val applicationController = createController(classOf[Application])
//  val applicationController: Application = inject [Application]

  "Main module" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val applicationController = Global.getControllerInstance(classOf[Application])
      val home = applicationController.index(FakeRequest().withCookies(cookie))

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Welcome")
    }
  }

}
