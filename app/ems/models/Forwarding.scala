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
  from: String, to: String, content: String, creationDate: DateTime,
  status: ForwardingStatus, mailgunId: String) extends MongoId(_id) {

  lazy val userId = _userId map { _.stringify }

  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")

  def withUser(_userId: BSONObjectID) = copy(_userId = Some(_userId))
  def withStatus(status: ForwardingStatus) = copy(status = status)
  def withMailgunId(mailgunId: String) = copy(mailgunId = mailgunId)
}


object Forwarding {
  implicit val forwardingStatusFormat = Json.format[ForwardingStatus]
  implicit val forwardingFormat = Json.format[Forwarding]
}
