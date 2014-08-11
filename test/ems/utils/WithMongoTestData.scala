package ems.utils


import _root_.securesocial.core.{AuthenticationMethod, BasicProfile}
import com.github.nscala_time.time.Imports.DateTime
import ems.backend.{UserInfoStore, UserStore, ForwardingStore}
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue

import ems.models.{UserInfo, User, Received, Forwarding}


/**
 * Provides data for mongo based tests
 */
trait WithMongoTestData {

  def generateId = BSONObjectID.generate

  lazy val data = Seq(
    (ForwardingStore.collectionName, forwardingJson),
    (UserStore.collectionName, userJson),
    (UserInfoStore.collectionName, userInfoJson)
  )

  // forwarding data
  lazy val forwardingId = "53cd93ce93d970b47bea76fd"
  lazy val mailgunId = "mailgunId"
  lazy val smsToEmailForwarding = Forwarding(
    BSONObjectID.parse(forwardingId).get,
    Some(BSONObjectID.parse(userMongoId).get),
    phoneNumber,
    Some("222222222"),
    "some text",
    DateTime.now,
    Received,
    mailgunId)
  lazy val forwardingList = List(smsToEmailForwarding)
  lazy val forwardingJson: List[JsValue] = forwardingList map {Forwarding.forwardingFormat.writes(_)}


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


  lazy val phoneNumber = "+15140000000"
  lazy val otherPhoneNumber = "+15140000001"

  lazy val userInfo = UserInfo(BSONObjectID.parse(userMongoId).get, Some(phoneNumber))
  lazy val otherUserInfo = UserInfo(BSONObjectID.parse(otherUserMongoId).get, Some(otherPhoneNumber))
  lazy val userInfoList = List(userInfo, otherUserInfo)
  lazy val userInfoJson: List[JsValue] = userInfoList map {UserInfo.userInfoFormat.writes(_)}
}
