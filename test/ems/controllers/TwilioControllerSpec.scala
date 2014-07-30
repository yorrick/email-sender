package ems.controllers


import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.runner._
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue
import play.api.test._

import ems.models.{SavedInMongo, Sms}
import ems.utils.WithMongoData


@RunWith(classOf[JUnitRunner])
class TwilioControllerSpec extends PlaySpecification {
  sequential

  val smsId = "53cd93ce93d970b47bea76fd"
  val smsList = List(Sms(BSONObjectID.parse(smsId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  val bsonList: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  val data = Seq(("smslist", bsonList))

  "Twilio controller" should {

    "Accept post data for sms" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val postResponse = ems.controllers.TwilioController.sms(request)
      status(postResponse) must equalTo(OK)
      println(contentAsString(postResponse))
      contentAsString(postResponse) must contain("Message")
    }

    "Reply with bad request if request is malformed" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "BodyXXX" -> "hello toto"
      )

      val postResponse = ems.controllers.TwilioController.sms(request)
      status(postResponse) must equalTo(BAD_REQUEST)
    }

  }
}
