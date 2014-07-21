package ems.models


/**
 * Object used to build forms to validate Mailgun POST requests for success deliveries
 */
case class MailgunEvent(messageId: String, event: String)
