package ems.models

import reactivemongo.bson.BSONObjectID
import com.github.nscala_time.time.Imports._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json


/**
 * Represent a Sms, used to create objects with forms when receiving data over http
 * @param from
 * @param to
 * @param content
 * @param creationDate
 */
case class Sms(_id: BSONObjectID, from: String, to: String, content: String,
               creationDate: DateTime, status: SmsStatus, mailgunId: String) {

  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")

  def withStatus(status: SmsStatus) = copy(status = status)
  def withMailgunId(mailgunId: String) = copy(mailgunId = mailgunId)
}


object Sms {
  implicit val smsStatusFormat = Json.format[SmsStatus]
  implicit val smsFormat = Json.format[Sms]
}
