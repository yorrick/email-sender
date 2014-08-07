package ems.controllers


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoTestData, WithMongoData}


@RunWith(classOf[JUnitRunner])
class TwilioControllerSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Twilio controller" should {

    "Accept post data for sms" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val postResponse = ems.controllers.TwilioController.receive(request)
      status(postResponse) must equalTo(OK)
      println(contentAsString(postResponse))
      contentAsString(postResponse) must beEqualTo("")
    }

    "Reply with bad request if request is malformed" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "BodyXXX" -> "hello toto"
      )

      val postResponse = ems.controllers.TwilioController.receive(request)
      status(postResponse) must equalTo(BAD_REQUEST)
    }

  }
}
