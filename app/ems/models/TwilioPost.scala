package ems.models


/**
 * Object used to build forms to validate Twilio POST requests
 */
case class TwilioPost(from: String, to: String, content: String)
