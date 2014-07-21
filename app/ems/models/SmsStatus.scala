package ems.models


/**
 * All status for an sms
 * @param status
 */
sealed case class SmsStatus(status: String)
object SavedInMongo extends SmsStatus("SavedInMongo")
object SentToMailgun extends SmsStatus("SentToMailgun")
object NotSentToMailgun extends SmsStatus("NotSentToMailgun")
object AckedByMailgun extends SmsStatus("AckedByMailgun")
object FailedByMailgun extends SmsStatus("FailedByMailgun")
