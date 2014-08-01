package ems.backend

import scala.concurrent.Future

import reactivemongo.api._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._

import ems.models._


/**
 * Handles sms storage in mongodb
 */
object SmsStore extends MongoDBStore {

  override val collectionName = "sms"

  /**
   * Save an sms
   * @param sms
   * @return
   */
  def save(sms: Sms): Future[Sms] = {
    collection.insert(sms) map {lastError => sms}
  }

  /**
   * Updates the status of an sms using the id
   * @param sms
   */
  def updateStatusById(sms: Sms): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> sms.status.status))
    updateById(sms, modifier)
  }

  /**
   * Ack a sms, given a mailgunId
   * Returns the acked sms
   * @param mailgunId
   */
  def updateStatusByMailgunId(mailgunId: String, status: SmsStatus): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> status.status))
    val findId = Json.obj("mailgunId" -> mailgunId)

    collection.update(findId, modifier) flatMap { lastError =>
      val cursor = collection.find(findId).cursor[Sms]
      // return the first result
      findSingle(cursor) map { _.get }
    }
  }

  /**
   * Set the mailgun id for an sms
   * @param sms
   */
  def updateSmsMailgunId(sms: Sms): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> sms.mailgunId))
    updateById(sms, modifier)
  }

  private def updateById(sms: Sms, modifier: JsObject) =
    collection.update(Json.obj("_id" -> sms._id), modifier) map {lastError => sms}

  /**
   * Returns the list of sms for the given user
   * @param userId
   * @return
   */
  def listSms(userId: String): Future[List[Sms]] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      val cursor: Cursor[Sms] = collection.
        // find all sms
        find(Json.obj("_userId" -> bsonId)).
        // sort by creation date
        sort(Json.obj("creationDate" -> -1)).
        // perform the query and get a cursor of JsObject
        cursor[Sms]

      // gather all the JsObjects in a list
      cursor.collect[List]()
    }
  }

}
