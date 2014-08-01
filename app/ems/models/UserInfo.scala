package ems.models


import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json


/**
 * Store all info related to User.
 * This data is not cached and does not have the same lifecycle as Users, so those objects are not
 * stored in the same collection as users.
 * @param _id match the user _id field
 * @param phoneNumber
 */
case class UserInfo(override val _id: BSONObjectID, phoneNumber: Option[String]) extends MongoId(_id)


object UserInfo {
  implicit val userInfoFormat = Json.format[UserInfo]
}