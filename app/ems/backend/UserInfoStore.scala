package ems.backend

import scala.concurrent.Future
import scala.util.{Success, Failure}

import play.api.libs.json._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._

import ems.models._


/**
 * Handles sms storage in mongodb
 */
object UserInfoStore extends MongoDBStore {

  override val collectionName = "userInfo"

  /**
   * Finds a user info by user id (or by _id, since they are the same)
   * @param userId
   * @return
   */
  def findUserInfoByUserId(userId: String): Future[UserInfo] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      val filter = Json.obj("_id" -> bsonId)
      findSingle(collection.find(filter).cursor[UserInfo]) map { _.get }
    }
  }

  /**
   * Finds a user info by phone number
   * @param phoneNumber
   * @return
   */
  def findUserInfoByPhoneNumber(phoneNumber: String): Future[UserInfo] = {
    val filter = Json.obj("phoneNumber" -> phoneNumber)
    findSingle(collection.find(filter).cursor[UserInfo]) map { _.get } andThen {
      case Success(user) =>
        Logger.debug(s"Found user info for incoming number $phoneNumber")
      case Failure(t) =>
        Logger.debug(s"Could not find any user info for number $phoneNumber")
    }
  }

  /**
   * Set the phone number for a user
   */
  def savePhoneNumber(userId: String, phoneNumber: String): Future[UserInfo] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      collection.update(
        Json.obj("_id" -> bsonId),
        Json.obj("$set" -> Json.obj("phoneNumber" -> phoneNumber))
      ) map { lastError => UserInfo(bsonId, Some(phoneNumber))}
    }
  }

}
