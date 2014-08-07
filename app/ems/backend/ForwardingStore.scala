package ems.backend


import ems.backend.UserStore._

import scala.concurrent.Future

import reactivemongo.api._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._

import ems.backend.utils.LogUtils
import ems.models._


/**
 * Handles forwarding storage in mongodb
 */
object ForwardingStore extends MongoDBStore with LogUtils {

  override val collectionName = "forwarding"

  /**
   * Save an forwarding
   * @param forwarding
   * @return
   */
  def save(forwarding: Forwarding): Future[Forwarding] = {
    collection.insert(forwarding) map {lastError => forwarding}
  }

  /**
   * Updates the status of an forwarding using the id
   * @param id
   * @param status
   */
  def updateStatusById(id: String, status: ForwardingStatus): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> status.status))

    for {
      bsonId <- toBSONObjectId(id)
      lastError <- collection.update(Json.obj("_id" -> bsonId), modifier)
      forwarding <- findForwardingById(id)
    } yield forwarding

  }

  /**
   * Set the mailgun id for an forwarding
   * @param id
   * @param mailgunId
   */
  def updateMailgunIdById(id: String, mailgunId: String): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> mailgunId))

    for {
      bsonId <- toBSONObjectId(id)
      lastError <- collection.update(Json.obj("_id" -> bsonId), modifier)
      forwarding <- findForwardingById(id)
    } yield forwarding

  }

  /**
   * Find a forwarding by id
   * @param id
   * @return
   */
  def findForwardingById(id: String): Future[Forwarding] = {
    toBSONObjectId(id) flatMap { bsonId =>
      val filter = Json.obj("_id" -> bsonId)
      findSingle(collection.find(filter).cursor[Forwarding]) map { _.get }
    }
  }

  /**
   * Update forwarding status, given a mailgunId
   * @param mailgunId
   */
  def updateStatusByMailgunId(mailgunId: String, status: ForwardingStatus): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> status.status))
    val findId = Json.obj("mailgunId" -> mailgunId)

    collection.update(findId, modifier) flatMap { lastError =>
      val cursor = collection.find(findId).cursor[Forwarding]
      // return the first result
      findSingle(cursor) map { _.get }
    } andThen logResult(s"updateStatusByMailgunId for mailgunId $mailgunId with status $status")
  }

  private def updateById(forwarding: Forwarding, modifier: JsObject) =
    collection.update(Json.obj("_id" -> forwarding._id), modifier) map {lastError => forwarding}

  /**
   * Returns the list of forwarding for the given user
   * @param userId
   * @return
   */
  def listForwarding(userId: String): Future[List[Forwarding]] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      val cursor: Cursor[Forwarding] = collection.
        // find all forwarding
        find(Json.obj("_userId" -> bsonId)).
        // sort by creation date
        sort(Json.obj("creationDate" -> -1)).
        // perform the query and get a cursor of JsObject
        cursor[Forwarding]

      // gather all the JsObjects in a list
      cursor.collect[List]()
    }
  }

}
