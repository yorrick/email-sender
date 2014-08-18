package ems.controllers


import ems.backend.Global
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.http.{MimeTypes, HeaderNames}
import play.api.Logger
import play.api.test._

import ems.utils.securesocial.WithSecureSocialUtils
import ems.utils.{WithMongoTestData, WithMongoApplication}


@RunWith(classOf[JUnitRunner])
class ForwardingControllerSpec extends PlaySpecification with WithSecureSocialUtils with WithMongoTestData {
  sequential
//  isolated

  step {
    Logger.info("Before class")
  }

  "Forwarding controller" should {

    "render the forwarding list page" in new WithMongoApplication(data) {

      val response = Global.getControllerInstance(classOf[ForwardingController]).list(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("some text")
    }

    "block access to non logged users" in new WithMongoApplication(data) {
      val response = Global.getControllerInstance(classOf[ForwardingController]).list(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.HTML))

      status(response) must equalTo(SEE_OTHER)
    }

  }

  step {
    Logger.info("After class")
  }
}
