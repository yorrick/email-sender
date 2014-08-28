package ems.backend.persistence

import ems.backend.persistence.mongo.MongoDBStore
import ems.backend.utils.LogUtils
import ems.models._
import play.api.libs.json._
import reactivemongo.api._
import play.modules.reactivemongo.json.BSONFormats._
import scaldi.{Injector, Injectable}

import scala.concurrent.{ExecutionContext, Future}


/**
 * Handles message storage in mongodb
 */
class MongoMessageStore(implicit inj: Injector) extends MongoDBStore with LogUtils with MessageStore with Injectable {

  override val collectionName = inject[String] (identified by "ems.backend.persistence.MongoMessageStore.collectionName")
  implicit val executionContext = inject[ExecutionContext]

  /**
   * Save an message
   * @param message
   * @return
   */
  def save(message: Message): Future[Message] = {
    collection.insert(message) map {lastError => message}
  }

  /**
   * Updates the status of an message using the id
   * @param id
   * @param status
   */
  def updateStatusById(id: String, status: MessageStatus): Future[Message] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> status.status))

    for {
      bsonId <- toBSONObjectId(id)
      lastError <- collection.update(Json.obj("_id" -> bsonId), modifier)
      message <- findMessageById(id)
    } yield message

  }

  /**
   * Set the mailgun id for an message
   * @param id
   * @param mailgunId
   */
  def updateMailgunIdById(id: String, mailgunId: String): Future[Message] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> mailgunId))

    for {
      bsonId <- toBSONObjectId(id)
      lastError <- collection.update(Json.obj("_id" -> bsonId), modifier)
      message <- findMessageById(id)
    } yield message

  }

  /**
   * Find a message by id
   * @param id
   * @return
   */
  def findMessageById(id: String): Future[Message] = {
    toBSONObjectId(id) flatMap { bsonId =>
      val filter = Json.obj("_id" -> bsonId)
      findSingle(collection.find(filter).cursor[Message])
    }
  }

  /**
   * Update message status, given a mailgunId
   * @param mailgunId
   */
  def updateStatusByMailgunId(mailgunId: String, status: MessageStatus): Future[Message] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> status.status))
    val findId = Json.obj("mailgunId" -> mailgunId)

    collection.update(findId, modifier) flatMap { lastError =>
      val cursor = collection.find(findId).cursor[Message]
      // return the first result
      findSingle(cursor)
    } andThen logResult(s"updateStatusByMailgunId for mailgunId $mailgunId with status $status")
  }

  private def updateById(message: Message, modifier: JsObject) =
    collection.update(Json.obj("_id" -> message._id), modifier) map {lastError => message}

  /**
   * Returns the list of message for the given user
   * @param userId
   * @return
   */
  def listMessage(userId: String): Future[List[Message]] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      val cursor: Cursor[Message] = collection.
        // find all message
        find(Json.obj("_userId" -> bsonId)).
        // sort by creation date
        sort(Json.obj("creationDate" -> -1)).
        // perform the query and get a cursor of JsObject
        cursor[Message]

      // gather all the JsObjects in a list
      cursor.collect[List]()
    }
  }

}
