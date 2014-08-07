package ems.backend

import reactivemongo.core.commands.LastError

import scala.concurrent.Future

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._

import ems.backend.utils.LogUtils
import ems.models._


/**
 * Handles forwarding storage in mongodb
 */
object UserInfoStore extends MongoDBStore with LogUtils {

  /**
   * Service exception
   * @param msg
   */
  case class UserInfoStoreException(val msg: String) extends Exception(msg)

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
    findSingle(collection.find(filter).cursor[UserInfo]) map { _.get } andThen logResult("findUserInfoByPhoneNumber")
  }

  /**
   * Creates the user info
   * @param userInfo
   * @return
   */
  def createUserInfo(userInfo: UserInfo): Future[UserInfo] =
    collection.insert(userInfo) map { _ => userInfo }

  /**
   * Set the phone number for a user
   */
  def savePhoneNumber(userId: String, phoneNumber: String): Future[UserInfo] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      collection.update(
        Json.obj("_id" -> bsonId),
        Json.obj("$set" -> Json.obj("phoneNumber" -> phoneNumber))
      ) transform (lastError => UserInfo(bsonId, Some(phoneNumber)), handleSaveError _)
    }

  }

  private def handleSaveError(t: Throwable): UserInfoStoreException = {
    t match {
      case e: LastError if e.code == Some(11000) =>
        UserInfoStoreException("This phone number is already used by somebody else")
      case _ =>
        UserInfoStoreException("Could not save phone number")
    }
  }

}
