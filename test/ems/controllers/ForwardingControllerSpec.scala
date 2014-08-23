package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.http.{MimeTypes, HeaderNames}
import play.api.Logger
import play.api.test._
import scala.concurrent.ExecutionContext.Implicits.global
import ems.utils.securesocial.WithSecureSocialUtils
import ems.utils.{WithTestData, WithMongoApplication}


@RunWith(classOf[JUnitRunner])
class ForwardingControllerSpec extends PlaySpecification with WithSecureSocialUtils with WithTestData {
  sequential
//  isolated

  step {
    Logger.info("Before class")
  }

  implicit val executionContext = global

  "Forwarding controller" should {

    "render the forwarding list page" in new WithMongoApplication(data, app) {

      val response = app.global.getControllerInstance(classOf[ForwardingController]).list(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("Hello from sms")
      contentAsString(response) must contain ("Hello from email")
    }

    "block access to non logged users" in new WithMongoApplication(data, app) {
      val response = app.global.getControllerInstance(classOf[ForwardingController]).list(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.HTML))

      status(response) must equalTo(SEE_OTHER)
    }

  }

  step {
    Logger.info("After class")
  }
}
