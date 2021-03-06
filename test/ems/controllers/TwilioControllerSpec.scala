package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{AppInjector, WithTestData, WithMongoApplication}
import scaldi.Injectable


@RunWith(classOf[JUnitRunner])
class TwilioControllerSpec extends PlaySpecification with WithTestData with AppInjector with Injectable {
  sequential

  "Twilio controller" should {

    "Accept post data for sms" in new WithMongoApplication(data) {
      implicit val injector = appInjector

      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val postResponse = inject[TwilioController].receive(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must beEqualTo("")
    }

    "Reply with bad request if request is malformed" in new WithMongoApplication(data) {
      implicit val injector = appInjector

      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "BodyXXX" -> "hello toto"
      )

      val postResponse = inject[TwilioController].receive(request)
      status(postResponse) must equalTo(BAD_REQUEST)
    }

  }
}
