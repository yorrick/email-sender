package ems.backend.persistence

import ems.backend.auth.ExternalUserService
import ems.backend.persistence.mongo.MongoDBStore
import ems.backend.utils.LogUtils
import ems.models.User
import scaldi.Injector
import securesocial.core.BasicProfile
import securesocial.core.services.SaveMode

import scala.concurrent.Future


trait UserStore extends ExternalUserService[User] {
  def find(providerId: String, userId: String): Future[Option[BasicProfile]]
  def findUser(userId: String): Future[Option[User]]
  def findUserById(id: String): Future[User]
  def findByEmail(email: String): Future[User]
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]]
  def save(profile: BasicProfile, mode: SaveMode): Future[User]
  def link(current: User, to: BasicProfile): Future[User]
}
