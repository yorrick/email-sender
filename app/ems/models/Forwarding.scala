package ems.models

import reactivemongo.bson.BSONObjectID
import com.github.nscala_time.time.Imports._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json


/**
 * Represent a Forwarding, eg either
 *  - a sms forwarded to email
 *  - an email forwarded to sms
 */
case class Forwarding(override val _id: BSONObjectID, _userId: Option[BSONObjectID],
  from: String, to: Option[String], content: String, creationDate: DateTime,
  status: ForwardingStatus, mailgunId: String) extends MongoId(_id) {

  lazy val userId = _userId map { _.stringify }

  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")

  def withUserAndEmail(user: User) = copy(_userId = Some(user._id), to = user.main.email)
  def withUserInfoAndPhone(userInfo: UserInfo) = copy(_userId = Some(userInfo._id), to = userInfo.phoneNumber)
  def withStatus(status: ForwardingStatus) = copy(status = status)
  def withMailgunId(mailgunId: String) = copy(mailgunId = mailgunId)

  lazy val emailToSms = from.contains("@")
  lazy val smsToEmail: Boolean = !emailToSms
}


object Forwarding {
  implicit val forwardingStatusFormat = Json.format[ForwardingStatus]
  implicit val forwardingFormat = Json.format[Forwarding]
}
