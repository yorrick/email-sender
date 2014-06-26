package models

import com.github.nscala_time.time.Imports._


object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for sms thanks to Json Macros
  implicit val smsFormat = Json.format[Sms]
  implicit val smsDisplayFormat = Json.format[SmsDisplay]
}


case class Sms(val from: String, val to: String, val content: String, val creationDate: DateTime) {
  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")
}


object SmsDisplay {
  def fromSms(sms: Sms) = SmsDisplay(sms.from, sms.to, sms.content, sms.formattedCreationDate)

  case class Mapping(val templateTag: String, val jsonName: String)
  object FromMapping extends Mapping("##From", "from")
  object ToMapping extends Mapping("##To", "to")
  object ContentMapping extends Mapping("##Content", "content")
  object CreationMapping extends Mapping("##Creation", "creationDate")

}

case class SmsDisplay(val from: String, val to: String, val content: String, val creationDate: String)
