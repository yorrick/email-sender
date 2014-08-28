package ems.models

import reactivemongo.bson.BSONObjectID


/**
 * Used to notify forwarder service of events from mailgun
 * @param mailgunId
 * @param status
 */
case class MailgunEvent(mailgunId: String, status: MessageStatus)
