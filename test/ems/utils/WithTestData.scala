package ems.utils


import _root_.securesocial.core.{AuthenticationMethod, BasicProfile}
import com.github.nscala_time.time.Imports.DateTime
import ems.backend.persistence.mongo.MongoDBUtils
import reactivemongo.bson.BSONObjectID
import play.api.libs.json.JsValue
import scala.language.postfixOps
import ems.models.{UserInfo, User, Received, Forwarding}


/**
 * Provides data for mongo based tests
 */
trait WithTestData extends MongoDBUtils {

  lazy val data = Seq(
    ("forwarding", forwardingJson),
    ("user", userJson),
    ("userInfo", userInfoJson)
  )

  // forwarding data
  lazy val smsToEmailForwardingId = "53cd93ce93d970b47bea76fd"
  lazy val smsToEmailMailgunId = "mailgunId"
  lazy val smsToEmailForwarding = Forwarding(
    BSONObjectID.parse(smsToEmailForwardingId).get,
    Some(BSONObjectID.parse(userMongoId).get),
    phoneNumber,
    Some("222222222"),
    "Hello from sms",
    DateTime.now,
    Received,
    smsToEmailMailgunId)
  
  lazy val emailToSmsForwardingId = "53cd93ce93d970b47bea7699"
  lazy val emailToSmsForwarding = Forwarding(
    BSONObjectID.parse(emailToSmsForwardingId).get,
    Some(BSONObjectID.parse(userMongoId).get),
    userEmail,
    Some(phoneNumber),
    "Hello from email",
    DateTime.now,
    Received,
    "")

  lazy val forwardingList = List(smsToEmailForwarding, emailToSmsForwarding)
  lazy val forwardingMap = forwardingList map {f => f.id -> f} toMap
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
