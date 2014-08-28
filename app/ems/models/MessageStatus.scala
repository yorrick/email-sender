package ems.models


/**
 * All status for an message
 * @param status
 */
sealed case class MessageStatus(status: String)


object Received extends MessageStatus("Received")
object Sending extends MessageStatus("Sending")
object Sent extends MessageStatus("Sent")
object Failed extends MessageStatus("Failed")
