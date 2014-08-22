package ems.backend.persistence

import ems.models.{ForwardingStatus, Forwarding}

import scala.concurrent.Future


object ForwardingStore {
  val collectionName = "forwarding"
}

trait ForwardingStore {
  def save(forwarding: Forwarding): Future[Forwarding]
  def updateStatusById(id: String, status: ForwardingStatus): Future[Forwarding]
  def updateMailgunIdById(id: String, mailgunId: String): Future[Forwarding]
  def findForwardingById(id: String): Future[Forwarding]
  def listForwarding(userId: String): Future[List[Forwarding]]
  def updateStatusByMailgunId(mailgunId: String, status: ForwardingStatus): Future[Forwarding]
}
