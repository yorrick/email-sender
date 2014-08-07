package ems.backend


import scala.concurrent.Future

import reactivemongo.api._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._

import ems.backend.utils.LogUtils
import ems.models._


/**
 * Handles sms storage in mongodb
 */
object SmsStore extends MongoDBStore with LogUtils {

  override val collectionName = "sms"

  /**
   * Save an sms
   * @param sms
   * @return
   */
  def save(sms: Forwarding): Future[Forwarding] = {
    collection.insert(sms) map {lastError => sms}
  }

  /**
   * Updates the status of an sms using the id
   * @param sms
   */
  def updateStatusById(sms: Forwarding): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> sms.status.status))
    updateById(sms, modifier)
  }

  /**
   * Ack a sms, given a mailgunId
   * Returns the acked forwarding
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
   * Set the mailgun id for an sms
   * @param sms
   */
  def updateSmsMailgunId(sms: Forwarding): Future[Forwarding] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> sms.mailgunId))
    updateById(sms, modifier)
  }

  private def updateById(sms: Forwarding, modifier: JsObject) =
    collection.update(Json.obj("_id" -> sms._id), modifier) map {lastError => sms}

  /**
   * Returns the list of sms for the given user
   * @param userId
   * @return
   */
  def listSms(userId: String): Future[List[Forwarding]] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      val cursor: Cursor[Forwarding] = collection.
        // find all sms
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
