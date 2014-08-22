package ems.controllers


import ems.backend.email.MailgunService
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import MailgunService._
import ems.utils.{WithMongoTestData, WithMongoApplication}


@RunWith(classOf[JUnitRunner])
class MailgunControllerSpec extends PlaySpecification with WithMongoTestData {
  sequential

  val rawEmailContent =
    """hello from email
      |
      |
      |2014-08-07 22:12 GMT-04:00 Yorrick Jansen <yorrick.email.sender@gmail.com>:
      |
      |> hello from email
      |>
      |>
      |> 2014-08-07 22:12 GMT-04:00 <+14387639474@app25130478.mailgun.org>:
      |>
      |>> Hello from.sms
      |>
      |>
      |>""".stripMargin

  "Mailgun controller" should {

    "Accept post data for delivery ack" in new WithMongoApplication(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "Message-Id" -> forwardingId,
        "event" -> DELIVERED
      )

      val postResponse = app.global.getControllerInstance(classOf[MailgunController]).event(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must equalTo("")
    }

    "Accept post data for email receiving" in new WithMongoApplication(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "from" -> "Somebody <somebody@example.com>",
        "recipient" -> "+5140000000@xxxx.mailgun.net",
        "subject" -> "A subject",
        "body-plain" -> rawEmailContent
      )

      val postResponse = app.global.getControllerInstance(classOf[MailgunController]).receive(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must equalTo("")
    }

    "Extract email" in new WithMongoApplication(data) {
      val result = app.global.getControllerInstance(classOf[MailgunController]).extractEmail("Somebody <somebody@example.com>")
      result must beSome.which(_ == "somebody@example.com")
    }

    "Extract content properly" in new WithMongoApplication(data) {
      val result = app.global.getControllerInstance(classOf[MailgunController]).extractContent("Re: Sms forwarding", rawEmailContent)
      result must beEqualTo("hello from email")

    }

  }

}
