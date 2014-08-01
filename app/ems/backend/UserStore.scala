package ems.backend


import scala.concurrent.Future

import reactivemongo.api.Cursor
import securesocial.core._
import securesocial.core.services.SaveMode
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsObject, Json}

import ems.models.{UserInfo, User}


/**
 * Handles user storage in mongodb
 */
object UserStore extends ExternalUserService[User] with MongoDBStore {

  override val collectionName = "user"

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and userId $userId")

    findUser(providerId, userId) map { userOption => userOption map { _.main } }
  }

  def findUser(providerId: String, userId: String): Future[Option[User]] = {
    Logger.debug(s"Trying to find a User by providerId $providerId and userId $userId")

    val filter = Json.obj("main.providerId" -> providerId, "main.userId" -> userId)
    findSingle(userCursor(filter))
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
          // TODO update the BasicProfile?
          Future.successful(user)
        case None =>
          // create the user, with no phone number for a start
          val id = generateId
          val userToInsert = User(id, profile)
          val userInfoToInsert = UserInfo(id, None)

          for {
            lastError <- collection.insert(userToInsert)
            userinfo <- UserInfoStore.createUserInfo(userInfoToInsert)
          } yield userToInsert
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
   * Creates a User cursor
   * @param filter
   * @return
   */
  protected def userCursor(filter: JsObject): Cursor[User] = collection.find(filter).cursor[User]
}




