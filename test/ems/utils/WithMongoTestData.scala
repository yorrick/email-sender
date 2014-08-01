package ems.utils


import _root_.securesocial.core.{AuthenticationMethod, BasicProfile}
import com.github.nscala_time.time.Imports.DateTime
import ems.backend.{UserInfoStore, UserStore, SmsStore}
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue

import ems.models.{UserInfo, User, SavedInMongo, Sms}


/**
 * Provides data for mongo based tests
 */
trait WithMongoTestData {

  lazy val data = Seq(
    (SmsStore.collectionName, smsJson),
    (UserStore.collectionName, userJson),
    (UserInfoStore.collectionName, userInfoJson)
  )

  // sms data
  lazy val smsId = "53cd93ce93d970b47bea76fd"
  lazy val mailgunId = "mailgunId"
  lazy val sms = Sms(
    BSONObjectID.parse(smsId).get,
    BSONObjectID.parse(userMongoId).get,
    "11111111",
    "222222222",
    "some text",
    DateTime.now,
    SavedInMongo,
    mailgunId)
  lazy val smsList = List(sms)
  lazy val smsJson: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}


  // user data
  lazy val userMongoId = "99cd93ce93d970b47bea76fd"
  lazy val otherUserMongoId = "33cd93ce93d970b47bea76fd"
  lazy val userId = "userid-12345"
  lazy val otherUserId = "other-userid-12345"
  lazy val providerId = "providerId"
  lazy val userEmail = "paul.watson@foobar.com"
  lazy val otherUserEmail = "other.user@foobar.com"

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

  lazy val otherProfile = BasicProfile(
    providerId,
    otherUserId,
    Some("Other"),
    Some("User"),
    Some("Other User"),
    Some(otherUserEmail),
    None,
    AuthenticationMethod.OAuth2,
    None,
    None,
    None
  )

  lazy val user = User(BSONObjectID.parse(userMongoId).get, profile)
  lazy val otherUser = User(BSONObjectID.parse(otherUserMongoId).get, otherProfile)
  lazy val userList = List(user, otherUser)
  lazy val userJson: List[JsValue] = userList map {User.userFormat.writes(_)}


  lazy val phoneNumber = "1-514-000-0000"
  lazy val otherPhoneNumber = "2-514-000-0000"

  lazy val userInfo = UserInfo(BSONObjectID.parse(userMongoId).get, Some(phoneNumber))
  lazy val otherUserInfo = UserInfo(BSONObjectID.parse(otherUserMongoId).get, Some(otherPhoneNumber))
  lazy val userInfoList = List(userInfo, otherUserInfo)
  lazy val userInfoJson: List[JsValue] = userInfoList map {UserInfo.userInfoFormat.writes(_)}
}
