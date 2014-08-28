package ems.backend.persistence

import ems.models.{MessageStatus, Message}

import scala.concurrent.Future


trait MessageStore {
  def save(message: Message): Future[Message]
  def updateStatusById(id: String, status: MessageStatus): Future[Message]
  def updateMailgunIdById(id: String, mailgunId: String): Future[Message]
  def findMessageById(id: String): Future[Message]
  def listMessage(userId: String): Future[List[Message]]
  def updateStatusByMailgunId(mailgunId: String, status: MessageStatus): Future[Message]
}
