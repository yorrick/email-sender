package ems.controllers


import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.runner._
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue
import play.api.test._

import ems.backend.Mailgun._
import ems.models.{SavedInMongo, Sms}
import ems.utils.WithMongoData


@RunWith(classOf[JUnitRunner])
class MailgunControllerSpec extends PlaySpecification {
  sequential

  val smsId = "53cd93ce93d970b47bea76fd"
  val smsList = List(Sms(BSONObjectID.parse(smsId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  val bsonList: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  val data = Seq(("smslist", bsonList))

  "Mailgun controller" should {

    "Accept post data for delivery ack" in new WithMongoData(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "Message-Id" -> smsId,
        "event" -> DELIVERED
      )

      val postResponse = ems.controllers.MailgunController.success(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must equalTo("")
    }

  }

}
