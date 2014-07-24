package ems.models


import securesocial.core.{BasicProfile, AuthenticationMethod, OAuth1Info, OAuth2Info, PasswordInfo}

import play.api.libs.json.Json


// a simple User class that has one identity
case class User(main: BasicProfile)


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
