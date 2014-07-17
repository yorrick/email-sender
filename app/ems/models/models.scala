package models

import akka.util.ByteString
import com.github.nscala_time.time.Imports._
import reactivemongo.bson.BSONObjectID
import redis.ByteStringFormatter
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._


sealed case class SmsStatus(status: String)
object NotSavedInMongo extends SmsStatus("NotSavedInMongo")
object SavedInMongo extends SmsStatus("SavedInMongo")
object SentToMailgun extends SmsStatus("SentToMailgun")
object NotSentToMailgun extends SmsStatus("NotSentToMailgun")
object AckedByMailgun extends SmsStatus("AckedByMailgun")


/**
 * Represent a Sms, used to create objects with forms when receiving data over http
 * @param from
 * @param to
 * @param content
 * @param creationDate
 */
case class Sms(_id: BSONObjectID, from: String, to: String, content: String, creationDate: DateTime, status: SmsStatus) {
  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")

  def withStatus(status: SmsStatus) = copy(status = status)
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
case class SmsDisplay(id: String, from: String, to: String, content: String, creationDate: String, status: String)


object SmsDisplay {
  def fromSms(sms: Sms) =
    SmsDisplay(sms._id.stringify, sms.from, sms.to, sms.content, sms.formattedCreationDate, sms.status.status)
  val empty = SmsDisplay("", "", "", "", "", "")

  case class Mapping(val templateTag: String, val jsonName: String)
  object IdMapping extends Mapping("##Id", "id")
  object FromMapping extends Mapping("##From", "from")
  object ToMapping extends Mapping("##To", "to")
  object ContentMapping extends Mapping("##Content", "content")
  object CreationMapping extends Mapping("##Creation", "creationDate")
  object StatusMapping extends Mapping("##Status", "status")

  implicit val smsDisplayFormat = Json.format[SmsDisplay]

  implicit val smsDisplayByteStringFormatter = new ByteStringFormatter[SmsDisplay] {
    def serialize(smsDisplay: SmsDisplay): ByteString = {
      ByteString(
        smsDisplay.id + "|" +
        smsDisplay.from + "|" +
        smsDisplay.to + "|" +
        smsDisplay.content + "|" +
        smsDisplay.creationDate + "|" +
        smsDisplay.status)
    }

    def deserialize(bs: ByteString): SmsDisplay = {
      val result = bs.utf8String.split('|').toList
      SmsDisplay(result(0), result(1), result(2), result(3), result(4), result(5))
    }
  }

}


case class Signal(content: String)

object Signal {
  implicit val signalFormat = Json.format[Signal]
}

object Ping extends Signal("ping")
