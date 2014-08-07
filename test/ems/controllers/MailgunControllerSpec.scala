package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.backend.Mailgun._
import ems.utils.{WithMongoTestData, WithMongoData}


@RunWith(classOf[JUnitRunner])
class MailgunControllerSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Mailgun controller" should {

    "Accept post data for delivery ack" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "Message-Id" -> forwardingId,
        "event" -> DELIVERED
      )

      val postResponse = ems.controllers.MailgunController.event(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must equalTo("")
    }

    "Accept post data for email receiving" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "from" -> "somebody@example.com",
        "recipient" -> "5140000000@xxxx.mailgun.net",
        "subject" -> "A subject",
        "body-plain" -> "Some plain content"
      )

      val postResponse = ems.controllers.MailgunController.receive(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must equalTo("")
    }

  }

}
