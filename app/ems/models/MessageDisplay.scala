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
case class MessageDisplay(id: String, userId: String, from: String, to: String, content: String,
                      creationDate: String, statusCode: String, status: String,
                      smsToEmail: String, emailToSms: String)


object MessageDisplay {

  /**
   * Creates an MessageDisplay object
   * @param message
   * @return
   */
  def fromMessage(message: Message) = MessageDisplay(
    message.id,
    message.userId.getOrElse(""),
    message.from,
    message.to.getOrElse(""),
    message.content,
    message.formattedCreationDate,
    message.status.status,
    statusLabels(message.status),
    message.smsToEmail.toString,
    message.emailToSms.toString
  )

  val statusLabels = Map(
    Received -> "Received",
    Sending -> "Sending",
    Sent -> "Sent",
    Failed -> "Failed"
  )

  val empty = MessageDisplay("", "", "No sms or emails have been sent yet", "", "", "", "", "", "", "")

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

  implicit val messageDisplayFormat = Json.format[MessageDisplay]

  /**
   * This formatter is used to serialize / deserialize MessageDisplay object in redis
   */
  implicit val messageDisplayByteStringFormatter = new ByteStringFormatter[MessageDisplay] {
    def serialize(messageDisplay: MessageDisplay): ByteString = {
      ByteString(
        messageDisplay.id + "|" +
        messageDisplay.userId + "|" +
        messageDisplay.from + "|" +
        messageDisplay.to + "|" +
        messageDisplay.content + "|" +
        messageDisplay.creationDate + "|" +
        messageDisplay.statusCode + "|" +
        messageDisplay.status + "|" +
        messageDisplay.smsToEmail + "|" +
        messageDisplay.emailToSms
      )
    }

    def deserialize(bs: ByteString): MessageDisplay = {
      val result = bs.utf8String.split('|').toList
      MessageDisplay(result(0), result(1), result(2), result(3), result(4), result(5), result(6),
        result(7), result(8), result(9))
    }
  }

}