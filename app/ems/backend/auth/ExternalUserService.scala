package ems.backend.auth

import ems.models.User
import securesocial.core.providers.MailToken
import securesocial.core.services.UserService
import securesocial.core.{BasicProfile, PasswordInfo}

import scala.concurrent.Future


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
