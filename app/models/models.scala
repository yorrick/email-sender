package models


object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for sms thanks to Json Macros
  implicit val smsFormat = Json.format[Sms]
}

// TODO add creation date
case class Sms(val from: String, val to: String, val content: String)
