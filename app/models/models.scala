package models

import com.github.nscala_time.time.Imports._
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder


object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for sms thanks to Json Macros
  implicit val smsFormat = Json.format[Sms]
}


case class Sms(val from: String, val to: String, val content: String, val creationDate: DateTime) {
  val formattedCreationDate = creationDate.toString("yyyy-MM-dd' 'HH:mm:ss")
}
