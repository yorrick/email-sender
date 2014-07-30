package ems.utils


import _root_.securesocial.core.{AuthenticationMethod, BasicProfile}
import com.github.nscala_time.time.Imports.DateTime
import ems.backend.SmsStore
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue

import ems.models.{User, SavedInMongo, Sms}


/**
 * Provides data for mongo based tests
 */
trait WithMongoTestData {

  // sms data
  lazy val smsId = "53cd93ce93d970b47bea76fd"
  lazy val smsList = List(Sms(BSONObjectID.parse(smsId).get, BSONObjectID.parse(userMongoId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  lazy val smsJson: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}


  // user data
  lazy val userMongoId = "99cd93ce93d970b47bea76fd"
  lazy val userId = "userid-12345"
  lazy val providerId = "providerId"
  lazy val userEmail = "paul.watson@foobar.com"
  lazy val userPhoneNumber = "1-514-000-0000"

  lazy val profile = BasicProfile(
    providerId,
    userId,
    Some("Paul"),
    Some("Watson"),
    Some("Paul Watson"),
    Some(userEmail),
    None,
    AuthenticationMethod.OAuth2,
    None,
    None,
    None
  )

  lazy val user = User(BSONObjectID.parse(userMongoId).get, profile, Some(userPhoneNumber))
  lazy val userList = List(user)
  lazy val userJson: List[JsValue] = userList map {User.userFormat.writes(_)}

  lazy val data = Seq(("smslist", smsJson), ("users", userJson))
}
