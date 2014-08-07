package ems.backend


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
   * @param forwarding
   */
  def updateStatusById(forwarding: Forwarding): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> forwarding.status.status))
    updateById(forwarding, modifier)
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

  /**
   * Set the mailgun id for an forwarding
   * @param forwarding
   */
  def updateForwardingMailgunId(forwarding: Forwarding): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> forwarding.mailgunId))
    updateById(forwarding, modifier)
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
