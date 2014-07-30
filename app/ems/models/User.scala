package ems.models


import reactivemongo.bson.BSONObjectID
import securesocial.core.{BasicProfile, AuthenticationMethod, OAuth1Info, OAuth2Info, PasswordInfo}
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json


// a simple User class that has one identity
case class User(
  _id: BSONObjectID,
  main: BasicProfile,
  phoneNumber: Option[String]
)


/**
 * Declares all json formats to store whole BasicProfile objects
 */
object User {
  implicit val authenticationMethodFormat = Json.format[AuthenticationMethod]
  implicit val oauth1InfoFormat = Json.format[OAuth1Info]
  implicit val oauth2InfoFormat = Json.format[OAuth2Info]
  implicit val passwordInfoFormat = Json.format[PasswordInfo]
  implicit val basicProfileFormat = Json.format[BasicProfile]
  implicit val userFormat = Json.format[User]
}
