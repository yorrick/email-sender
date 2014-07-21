package ems.models

import akka.util.ByteString
import com.github.nscala_time.time.Imports._
import reactivemongo.bson.BSONObjectID
import redis.ByteStringFormatter
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._


/**
 * Object used to build forms to validate Mailgun POST requests for success deliveries
 */
case class MailgunEvent(messageId: String, event: String)


/**
 * All status for an sms
 * @param status
 */
sealed case class SmsStatus(status: String)
object NotSavedInMongo extends SmsStatus("NotSavedInMongo")
object SavedInMongo extends SmsStatus("SavedInMongo")
object SentToMailgun extends SmsStatus("SentToMailgun")
object NotSentToMailgun extends SmsStatus("NotSentToMailgun")
object AckedByMailgun extends SmsStatus("AckedByMailgun")
object FailedByMailgun extends SmsStatus("FailedByMailgun")


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


/**
 * Used in templates for display
 * @param from
 * @param to
 * @param content
 * @param creationDate
 */
case class SmsDisplay(id: String, from: String, to: String, content: String,
                      creationDate: String, statusCode: String, status: String, spin: String)


object SmsDisplay {

  /**
   * Creates an SmsDisplay object
   * @param sms
   * @return
   */
  def fromSms(sms: Sms) = SmsDisplay(
    sms._id.stringify,
    sms.from,
    sms.to,
    sms.content,
    sms.formattedCreationDate,
    sms.status.status,
    statusLabels(sms.status)._1,
    statusLabels(sms.status)._2
  )

  val statusLabels = Map(
    NotSavedInMongo -> ("Not received", "false"),
    SavedInMongo -> ("Received", "true"),
    SentToMailgun -> ("Sending", "true"),
    NotSentToMailgun -> ("Failed", "false"),
    AckedByMailgun -> ("Delivered", "false")
  )
  
  val empty = SmsDisplay("", "", "", "", "", "", "false", "")

  case class Mapping(val templateTag: String, val jsonName: String)
  object IdMapping extends Mapping("##Id", "id")
  object FromMapping extends Mapping("##From", "from")
  object ToMapping extends Mapping("##To", "to")
  object ContentMapping extends Mapping("##Content", "content")
  object CreationMapping extends Mapping("##Creation", "creationDate")
  object StatusCodeMapping extends Mapping("##StatusCode", "statusCode")
  object StatusMapping extends Mapping("##Status", "status")
  object SpinMapping extends Mapping("##Spin", "spin")

  implicit val smsDisplayFormat = Json.format[SmsDisplay]

  implicit val smsDisplayByteStringFormatter = new ByteStringFormatter[SmsDisplay] {
    def serialize(smsDisplay: SmsDisplay): ByteString = {
      ByteString(
        smsDisplay.id + "|" +
        smsDisplay.from + "|" +
        smsDisplay.to + "|" +
        smsDisplay.content + "|" +
        smsDisplay.creationDate + "|" +
        smsDisplay.statusCode + "|" +
        smsDisplay.status + "|" +
        smsDisplay.spin
      )
    }

    def deserialize(bs: ByteString): SmsDisplay = {
      val result = bs.utf8String.split('|').toList
      SmsDisplay(result(0), result(1), result(2), result(3), result(4), result(5), result(6), result(7))
    }
  }

}


case class Signal(content: String)

object Signal {
  implicit val signalFormat = Json.format[Signal]
}

object Ping extends Signal("ping")
