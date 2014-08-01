package ems.utils.securesocial


import scala.concurrent.Future
import scala.reflect.ClassTag

import com.github.nscala_time.time.Imports._
import securesocial.core.authenticator.{CookieAuthenticator, AuthenticatorStore}

import ems.models.User


/**
 * A store that always returns the same User
 */
class MockAuthenticatorStore(val user: User) extends AuthenticatorStore[CookieAuthenticator[User]] {

  val authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
    "emailsenderid", user, DateTime.nextDay, DateTime.now, DateTime.lastDay, this)

  def find(id: String)(implicit ct: ClassTag[CookieAuthenticator[User]]): Future[Option[CookieAuthenticator[User]]] = {
    Future.successful(Some(authenticator))
  }

  def save(authenticator: CookieAuthenticator[User], timeoutInSeconds: Int): Future[CookieAuthenticator[User]] = {
    Future.successful(authenticator)
  }

  def delete(id: String): Future[Unit] = Future.successful(Unit)
}
