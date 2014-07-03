package models

import akka.util.ByteString
import com.github.nscala_time.time.Imports._
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

  implicit val smsDisplayFormat = Json.format[SmsDisplay]

}


/**
 * Used in templates for display
 * @param from
 * @param to
 * @param content
 * @param creationDate
 */
case class SmsDisplay(val from: String, val to: String, val content: String, val creationDate: String)


case class Signal(content: String)

object Signal {
  implicit val signalFormat = Json.format[Signal]
}

object Ping extends Signal("ping")
