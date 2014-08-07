package ems.models


/**
 * All status for an sms
 * @param status
 */
sealed case class ForwardingStatus(status: String)
object SavedInMongo extends ForwardingStatus("SavedInMongo")
object SentToMailgun extends ForwardingStatus("SentToMailgun")
object NotSentToMailgun extends ForwardingStatus("NotSentToMailgun")
object AckedByMailgun extends ForwardingStatus("AckedByMailgun")
object FailedByMailgun extends ForwardingStatus("FailedByMailgun")
