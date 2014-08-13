package ems.backend


import scala.concurrent.Future

import reactivemongo.api.Cursor
import securesocial.core._
import securesocial.core.services.SaveMode
import play.api.Logger
import play.modules.reactivemongo.json.BSONFormats._
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
    findSingle(userCursor(filter)) map { Some(_) }
  }

  def findUserById(id: String): Future[User] = {
    toBSONObjectId(id) flatMap { bsonId =>
      val filter = Json.obj("_id" -> bsonId)
      findSingle(userCursor(filter))
    }
  }

  def findByEmail(email: String): Future[User] = {
    // TODO We could maybe add a unique constraint on email to be able to find users by email for sure
    Logger.debug(s"Trying to find a User by email $email")

    val filter = Json.obj("main.email" -> email)
    findSingle(userCursor(filter))
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and email $email")

    val filter = Json.obj("main.providerId" -> providerId, "main.email" -> email)
    findSingle(userCursor(filter)) map { user => Some(user.main) }
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




