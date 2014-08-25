//package ems.utils.securesocial
//
//import com.github.nscala_time.time.Imports._
//import ems.models.User
//import ems.utils.WithTestData
//import securesocial.core.authenticator.{AuthenticatorStore, CookieAuthenticator}
//
//import scala.concurrent.Future
//import scala.reflect.ClassTag
//
//
///**
//* A store that always returns the same User
//*/
//class MockAuthenticatorStore extends AuthenticatorStore[CookieAuthenticator[User]] with WithTestData {
//
//  val authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
//    "emailsenderid", user, DateTime.nextDay, DateTime.now, DateTime.lastDay, this)
//
//  def find(id: String)(implicit ct: ClassTag[CookieAuthenticator[User]]): Future[Option[CookieAuthenticator[User]]] = {
//    Future.successful(Some(authenticator))
//  }
//
//  def save(authenticator: CookieAuthenticator[User], timeoutInSeconds: Int): Future[CookieAuthenticator[User]] = {
//    Future.successful(authenticator)
//  }
//
//  def delete(id: String): Future[Unit] = Future.successful(Unit)
//}
