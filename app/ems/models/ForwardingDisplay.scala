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
case class ForwardingDisplay(id: String, userId: String, from: String, to: String, content: String,
                      creationDate: String, statusCode: String, status: String,
                      smsToEmail: String, emailToSms: String)


object ForwardingDisplay {

  /**
   * Creates an ForwardingDisplay object
   * @param forwarding
   * @return
   */
  def fromForwarding(forwarding: Forwarding) = ForwardingDisplay(
    forwarding.id,
    forwarding.userId.getOrElse(""),
    forwarding.from,
    forwarding.to.getOrElse(""),
    forwarding.content,
    forwarding.formattedCreationDate,
    forwarding.status.status,
    statusLabels(forwarding.status),
    forwarding.smsToEmail.toString,
    forwarding.emailToSms.toString
  )

  val statusLabels = Map(
    Received -> "Received",
    Sending -> "Sending",
    Sent -> "Sent",
    Failed -> "Failed"
  )

  val empty = ForwardingDisplay("", "", "No sms or emails have been sent yet", "", "", "", "", "", "", "")

  case class Mapping(val templateTag: String, val jsonName: String)
  object IdMapping extends Mapping("##Id", "id")
  object UserIdMapping extends Mapping("##UserId", "userId")
  object FromMapping extends Mapping("##From", "from")
  object ToMapping extends Mapping("##To", "to")
  object ContentMapping extends Mapping("##Content", "content")
  object CreationMapping extends Mapping("##Creation", "creationDate")
  object StatusCodeMapping extends Mapping("##StatusCode", "statusCode")
  object StatusMapping extends Mapping("##Status", "status")
  object SmsToEmailMapping extends Mapping("##SmsToEmail", "smsToEmail")
  object EmailToSmsMapping extends Mapping("##EmailToSms", "emailToSms")

  implicit val forwardingDisplayFormat = Json.format[ForwardingDisplay]

  /**
   * This formatter is used to serialize / deserialize ForwardingDisplay object in redis
   */
  implicit val forwardingDisplayByteStringFormatter = new ByteStringFormatter[ForwardingDisplay] {
    def serialize(forwardingDisplay: ForwardingDisplay): ByteString = {
      ByteString(
        forwardingDisplay.id + "|" +
        forwardingDisplay.userId + "|" +
        forwardingDisplay.from + "|" +
        forwardingDisplay.to + "|" +
        forwardingDisplay.content + "|" +
        forwardingDisplay.creationDate + "|" +
        forwardingDisplay.statusCode + "|" +
        forwardingDisplay.status + "|" +
        forwardingDisplay.smsToEmail + "|" +
        forwardingDisplay.emailToSms
      )
    }

    def deserialize(bs: ByteString): ForwardingDisplay = {
      val result = bs.utf8String.split('|').toList
      ForwardingDisplay(result(0), result(1), result(2), result(3), result(4), result(5), result(6),
        result(7), result(8), result(9))
    }
  }

}