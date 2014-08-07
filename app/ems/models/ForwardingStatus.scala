package ems.models


/**
 * All status for an forwarding
 * @param status
 */
sealed case class ForwardingStatus(status: String)


object Received extends ForwardingStatus("Received")
object Sending extends ForwardingStatus("Sending")
object Sent extends ForwardingStatus("Sent")
object Failed extends ForwardingStatus("Failed")
