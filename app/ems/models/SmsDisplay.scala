package ems.models

import akka.util.ByteString
import redis.ByteStringFormatter
import play.api.libs.json.Json


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
    SavedInMongo -> ("Received", "true"),
    SentToMailgun -> ("Sending", "true"),
    NotSentToMailgun -> ("Failed", "false"),
    AckedByMailgun -> ("Delivered", "false"),
    FailedByMailgun -> ("Failed", "false")
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

  /**
   * This formatter is used to serialize / deserialize SmsDisplay object in redis
   */
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