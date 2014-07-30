package ems.utils


import com.github.nscala_time.time.Imports.DateTime
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue

import ems.models.{SavedInMongo, Sms}


/**
 * Provides data for mongo based tests
 */
trait WithMongoTestData {

  lazy val smsId = "53cd93ce93d970b47bea76fd"
  lazy val smsList = List(Sms(BSONObjectID.parse(smsId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  lazy val json: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  lazy val data = Seq(("smslist", json))

}
