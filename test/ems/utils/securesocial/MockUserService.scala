package ems.utils.securesocial

import ems.backend.auth.ExternalUserService
import securesocial.core.services.SaveMode
import securesocial.core.BasicProfile

import scala.concurrent.Future


import ems.models.User


/**
 * A user service that always returns the same User
 */
class MockUserService extends ExternalUserService[User] {
  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Future.failed(new NotImplementedError())
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Future.failed(new NotImplementedError())
  }

  def save(user: BasicProfile, mode: SaveMode): Future[User] = {
    Future.failed(new NotImplementedError())
  }

  def link(current: User, to: BasicProfile): Future[User] = {
    Future.failed(new NotImplementedError())
  }

}