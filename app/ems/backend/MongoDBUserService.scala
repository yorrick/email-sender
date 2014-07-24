package ems.backend


import scala.concurrent.Future

import reactivemongo.bson.BSONObjectID
import securesocial.core._
import securesocial.core.providers.MailToken
import securesocial.core.services.{UserService, SaveMode}

import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import ems.models.User


/**
 * Provides empty implementations for everything that is related to UsernamePassword authentication
 * @tparam U
 */
trait ExternalUserService[U] extends UserService[U] {

  def saveToken(token: MailToken): Future[MailToken] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented"))
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented"))
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented"))
  }

  //  def deleteTokens(): Future {
  //    tokens = Map()
  //  }

  def deleteExpiredTokens() {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented"))
  }

  def updatePasswordInfo(user: User, info: PasswordInfo): Future[Option[BasicProfile]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented"))
  }

  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = {
    // not implemented since we do not use UsernamePassword provider
    Future.failed(new Exception("not implemented"))
  }
}


/**
 * A service that stores users in mongodb
 */
class MongoDBUserService extends ExternalUserService[User] {

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("users")
  def generateId = BSONObjectID.generate

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and userId $userId")

    findUser(providerId, userId) map { userOption => userOption map { _.main } }
  }

  def findUser(providerId: String, userId: String): Future[Option[User]] = {
    Logger.debug(s"Trying to find a User by providerId $providerId and userId $userId")

    val filter = Json.obj(
      "main.providerId" -> providerId,
      "main.userId" -> userId
    )

    val cursor = collection.find(filter).cursor[User]

    cursor.collect[List]() map {
      case user :: Nil =>
        Some(user)
      case _ =>
        None
    }

  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and email $email")

    val filter = Json.obj(
      "main.providerId" -> providerId,
      "main.email" -> email  // TODO check that None is represented bu null in json
    )

    val cursor = collection.find(filter).cursor[User]

    cursor.collect[List]() map {
      case user :: Nil => Some(user.main)
      case _ => None
    }

  }

  def save(user: BasicProfile, mode: SaveMode): Future[User] = {
    // first see if there is a user with this BasicProfile already.
    findUser(user.providerId, user.userId) flatMap { userOption =>
      userOption match {
        case Some(user) =>
          // TODO update the user?
          Future.successful(user)
        case None =>
          // create the user
          val userToInsert = User(user)
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
    Future.failed(new Exception("not implemented"))
  }

}




