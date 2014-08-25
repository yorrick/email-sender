package ems.backend.persistence

import ems.backend.persistence.mongo.MongoDBStore
import ems.backend.utils.LogUtils
import ems.models._
import play.api.libs.json._
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.BSONFormats._
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContext, Future}




/**
 * Handles forwarding storage in mongodb
 */
class MongoUserInfoStore(implicit inj: Injector) extends MongoDBStore with LogUtils with Injectable with UserInfoStore {

  override val collectionName = inject[String] (identified by "store.userInfo.collectionName")
  implicit val executionContext = inject[ExecutionContext]

  /**
   * Finds a user info by user id (or by _id, since they are the same)
   * @param userId
   * @return
   */
  def findUserInfoByUserId(userId: String): Future[UserInfo] = {
    toBSONObjectId(userId) flatMap { bsonId =>
      val filter = Json.obj("_id" -> bsonId)
      findSingle(collection.find(filter).cursor[UserInfo])
    }
  }

  /**
   * Finds a user info by phone number
   * @param phoneNumber
   * @return
   */
  def findUserInfoByPhoneNumber(phoneNumber: String): Future[UserInfo] = {
    val filter = Json.obj("phoneNumber" -> phoneNumber)
    findSingle(collection.find(filter).cursor[UserInfo]) andThen logResult("findUserInfoByPhoneNumber")
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
