package models

import akka.util.ByteString
import com.github.nscala_time.time.Imports._
import redis.ByteStringFormatter
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.json.Json


/**
 * Represent a Sms, used to create objects with forms when receiving data over http
 * @param from
 * @param to
 * @param content
 * @param creationDate
 */
case class Sms(val from: String, val to: String, val content: String, val creationDate: DateTime) {
  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")
}


object Sms {
  implicit val smsFormat = Json.format[Sms]
}


object SmsDisplay {
  def fromSms(sms: Sms) = SmsDisplay(sms.from, sms.to, sms.content, sms.formattedCreationDate)
  val empty = SmsDisplay("", "", "", "")

  case class Mapping(val templateTag: String, val jsonName: String)
  object FromMapping extends Mapping("##From", "from")
  object ToMapping extends Mapping("##To", "to")
  object ContentMapping extends Mapping("##Content", "content")
  object CreationMapping extends Mapping("##Creation", "creationDate")

  // used to send those objects through websockets
  implicit val smsDisplayFormat = Json.format[SmsDisplay]
  // used to send those objects through websockets (needs a JsonFormatter)
  implicit val smsDisplayFrameFormatter = FrameFormatter.jsonFrame[SmsDisplay]

  implicit val smsDisplayByteStringFormatter = new ByteStringFormatter[SmsDisplay] {
    def serialize(smsDisplay: SmsDisplay): ByteString = {
      ByteString(smsDisplay.from + "|" + smsDisplay.to + "|" + smsDisplay.content + "|" + smsDisplay.creationDate)
    }

    def deserialize(bs: ByteString): SmsDisplay = {
      val result = bs.utf8String.split('|').toList
      SmsDisplay(result(0), result(1), result(2), result(3))
    }
  }

}


/**
 * Used in templates for display
 * @param from
 * @param to
 * @param content
 * @param creationDate
 */
case class SmsDisplay(val from: String, val to: String, val content: String, val creationDate: String)


//sealed case class Signal(content: String) {
//  implicit val smsDisplayFormat = Json.format[Signal]
//  implicit val pingFrameFormatter = FrameFormatter.jsonFrame[Signal]
//}
//
//object ping extends Signal("ping")
