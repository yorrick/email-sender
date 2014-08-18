package ems.backend.utils

import ems.models.User
import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import scaldi.{Injector, Injectable}
import securesocial.core.authenticator._

import scala.concurrent.{ExecutionContext, Future}


class EMSCookieAuthenticatorBuilder(implicit inj: Injector) extends AuthenticatorBuilder[User] with Injectable {
  val id = CookieAuthenticator.Id

  val store = inject[AuthenticatorStore[CookieAuthenticator[User]]]
  val generator = inject[IdGenerator]

  /**
   * Creates an instance of a CookieAuthenticator from the http request
   *
   * @param request the incoming request
   * @return an optional CookieAuthenticator instance.
   */
  override def fromRequest(request: RequestHeader): Future[Option[CookieAuthenticator[User]]] = {
    import ExecutionContext.Implicits.global
    request.cookies.get(CookieAuthenticator.cookieName) match {
      case Some(cookie) => store.find(cookie.value).map { retrieved =>
        retrieved.map { _.copy(store = store) }
      }
      case None => Future.successful(None)
    }
  }

  /**
   * Creates an instance of a CookieAuthenticator from a user object.
   *
   * @param user the user
   * @return a CookieAuthenticator instance.
   */
  override def fromUser(user: User): Future[CookieAuthenticator[User]] = {
    import ExecutionContext.Implicits.global
    generator.generate.flatMap {
      id =>
        val now = DateTime.now()
        val expirationDate = now.plusMinutes(CookieAuthenticator.absoluteTimeout)
        val authenticator = CookieAuthenticator(id, user, expirationDate, now, now, store)
        store.save(authenticator, CookieAuthenticator.absoluteTimeoutInSeconds)
    }
  }
}
