package ems.backend


import reactivemongo.api.Cursor

import scala.concurrent.Future
import scala.util.{Failure, Success}

import reactivemongo.bson.BSONObjectID
import securesocial.core._
import securesocial.core.providers.MailToken
import securesocial.core.services.{UserService, SaveMode}

import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsObject, Json}

import ems.models.User


/**
 * Provides empty implementations for everything that is related to UsernamePassword authentication
 * @tparam U
 */
trait ExternalUserService[U] extends UserService[U] {

  def saveToken(token: MailToken): Future[MailToken] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented yet"))
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented yet"))
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented yet"))
  }

  //  def deleteTokens(): Future {
  //    tokens = Map()
  //  }

  def deleteExpiredTokens() {
    // not implemented since we do not use UsernamePassword provider
    throw new Exception("not implemented yet")
  }

  def updatePasswordInfo(user: User, info: PasswordInfo): Future[Option[BasicProfile]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented yet"))
  }

  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented yet"))
  }
}


/**
 * A service that stores users in mongodb
 * TODO merge this with MongoDB
 */
object MongoDBUserService extends ExternalUserService[User] {

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("users")
  def generateId = BSONObjectID.generate

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and userId $userId")

    findUser(providerId, userId) map { userOption => userOption map { _.main } }
  }

  def findUser(providerId: String, userId: String): Future[Option[User]] = {
    Logger.debug(s"Trying to find a User by providerId $providerId and userId $userId")

    val filter = Json.obj("main.providerId" -> providerId, "main.userId" -> userId)
    findSingle(userCursor(filter))
  }

  def findUserByPhoneNumber(phoneNumber: String): Future[User] = {
    Logger.debug(s"Trying to find a User by phone number $phoneNumber")

    val filter = Json.obj("phoneNumber" -> phoneNumber)
    findSingle(userCursor(filter)) map { _.get } andThen {
      case Success(user) =>
        Logger.debug(s"Found user for incoming number $phoneNumber")
      case Failure(t) =>
        Logger.debug(s"Could not create any sms for incoming number $phoneNumber")
    }
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and email $email")

    val filter = Json.obj("main.providerId" -> providerId, "main.email" -> email)
    findSingle(userCursor(filter)) map { basicProfileOption => basicProfileOption map { _.main} }
  }

  def save(profile: BasicProfile, mode: SaveMode): Future[User] = {
    // first see if there is a user with this BasicProfile already.
    findUser(profile.providerId, profile.userId) flatMap { userOption =>
      userOption match {
        case Some(user) =>
          // TODO update the user?
          Future.successful(user)
        case None =>
          // create the user, with no phone number for now
          // TODO remove the test phone number once we can update phone number in app
          val userToInsert = User(SmsStore.generateId, profile, Some("11111111"))
          collection.insert(userToInsert) map {lastError => userToInsert}
      }
    }
  }

  /**
   * We do not support multiple identities
   * @param current
   * @param to
   * @return
   */
  def link(current: User, to: BasicProfile): Future[User] = {
    Future.failed(new Exception("not implemented yet"))
  }

  /**
   * Finds a single result if there is only one, or None
   * @param cursor
   * @tparam T
   * @return
   */
  protected def findSingle[T](cursor: Cursor[T]): Future[Option[T]] = cursor.collect[List]() map {
    case element :: Nil => Some(element)
    case _ => None
  }

  /**
   * Creates a User cursor
   * @param filter
   * @return
   */
  protected def userCursor(filter: JsObject): Cursor[User] = collection.find(filter).cursor[User]
}




