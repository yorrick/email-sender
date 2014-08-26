package ems.backend.persistence

import ems.backend.persistence.mongo.MongoDBStore
import ems.backend.utils.LogUtils
import ems.models.{User, UserInfo}
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.Cursor
import scaldi.{Injectable, Injector}
import securesocial.core._
import securesocial.core.services.SaveMode
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.{ExecutionContext, Future}


/**
 * Handles user storage in mongodb.
 * Users are also stored in Redis cache.
 */
class MongoUserStore(implicit inj: Injector) extends UserStore with MongoDBStore with LogUtils with Injectable {

  val userInfoStore = inject[UserInfoStore]
  implicit val executionContext = inject[ExecutionContext]

  override val collectionName = inject[String] (identified by "ems.backend.persistence.MongoUserStore.collectionName")

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and userId $userId")

    val filter = Json.obj("main.providerId" -> providerId, "main.userId" -> userId)
    findSingle(userCursor(filter)) map { user => Some(user.main) } recover { case _ => None }
  }

  def findUser(userId: String): Future[Option[User]] = {
    Logger.debug(s"Trying to find a User by userId $userId")

    val filter = Json.obj("main.userId" -> userId)
    findSingle(userCursor(filter)) map { Some(_) } recover { case _ => None }
  }

  def findUserById(id: String): Future[User] = {
    toBSONObjectId(id) flatMap { bsonId =>
      val filter = Json.obj("_id" -> bsonId)
      findSingle(userCursor(filter))
    }
  }

  def findByEmail(email: String): Future[User] = {
    Logger.debug(s"Trying to find a User by email $email")

    val filter = Json.obj("main.email" -> email)
    findSingle(userCursor(filter))
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.debug(s"Trying to find a BasicProfile by providerId $providerId and email $email")

    val filter = Json.obj("main.providerId" -> providerId, "main.email" -> email)
    findSingle(userCursor(filter)) map { user => Some(user.main) } recover { case _ => None }
  }

  /**
   * Update provider if necessary: this can happen if a user logs with same email via different services
   * @param user
   * @param newProfile
   * @return
   */
  private def updateProfile(user: User, newProfile: BasicProfile): Future[User] = {
    val updates = Json.obj(
      "main.providerId" -> newProfile.providerId,
      "main.userId" -> newProfile.userId,
      "main.firstName" -> newProfile.firstName,
      "main.lastName" -> newProfile.lastName,
      "main.fullName" -> newProfile.fullName,
      "main.avatarUrl" -> newProfile.avatarUrl,
      "main.authMethod.method" -> newProfile.authMethod.method,
      "main.oAuth2Info.accessToken" -> newProfile.oAuth2Info.get.accessToken,
      "main.oAuth2Info.tokenType" -> newProfile.oAuth2Info.get.tokenType,
      "main.oAuth2Info.expiresIn" -> newProfile.oAuth2Info.get.expiresIn
    )

    val modifier = Json.obj("$set" -> updates)
    collection.update(Json.obj("_id" -> user._id), modifier) flatMap { _ => findUser(newProfile.userId) map { _.get } }
  }

  private def createUser(profile: BasicProfile): Future[User] = {
    // create the user, with no phone number for a start
    val id = generateId
    val userToInsert = User(id, profile)
    val userInfoToInsert = UserInfo(id, None)

    for {
      lastError <- collection.insert(userToInsert)
      userinfo <- userInfoStore.createUserInfo(userInfoToInsert)
    } yield userToInsert
  }

  def save(profile: BasicProfile, mode: SaveMode): Future[User] = {
    // first see if there is a user with this email already
    val user = findByEmail(profile.email.get) andThen logResult("findByEmail")

    user flatMap {
      user => updateProfile(user, profile) andThen logResult("updateProfile")
    } recoverWith {
      case _ => createUser(profile) andThen logResult("createUser")
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




