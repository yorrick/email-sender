package ems.utils.securesocial

import _root_.securesocial.core.RuntimeEnvironment
import _root_.securesocial.core.authenticator.{Authenticator, AuthenticatorBuilder}
import _root_.securesocial.core.services.{AuthenticatorService}
import com.github.nscala_time.time.Imports._
import ems.models.User
import ems.utils.{MockUtils, WithTestData}
import play.api.mvc.{Results, Result, RequestHeader, Cookie}
import play.mvc.Http.Context
import scala.concurrent.Future


/**
 * Contains mocks that are used in the tests
 */
trait AuthenticationUtils { self: WithTestData with MockUtils =>

  /**
   * Cookie used for tests.
   * The value is not used as the AuthenticationStore is mocked
   * TODO find a way to use configuration (we need an app in context to be able to use CookieAuthenticator.cookieName)
   */
  lazy val cookie = Cookie("emailsenderid", "")

  /**
   * A runtime env that disables the authentication
   */
  def mockRuntimeEnvironment = new RuntimeEnvironment.Default[User] {
    override lazy val userService = mockUserStore
    override lazy val authenticatorService = new AuthenticatorService(mockAuthenticatorBuilder)
  }

  def mockAuthenticatorBuilder = new AuthenticatorBuilder[User] {
    val authenticator = mockAuthenticator

    def fromRequest(request: RequestHeader): Future[Option[Authenticator[User]]] = Future.successful(Some(authenticator))

    def fromUser(user: User): Future[Authenticator[User]] = Future.successful(authenticator)

    override val id: String = "mockAuthenticatorBuilder"
  }

  def mockAuthenticator = new Authenticator[User] with Results {
    override val id: String = "mockAuthenticator"

    override def touch: Future[Authenticator[User]] = Future.successful(this)

    override def updateUser(user: User): Future[Authenticator[User]] = Future.successful(this)

    override def discarding(result: Result): Future[Result] = Future.successful(result)

    override def discarding(javaContext: Context): Future[Unit] = Future.successful(Unit)

    override def touching(result: Result): Future[Result] = Future.successful(result)

    override def touching(javaContext: Context): Future[Unit] = Future.successful(Unit)

    override def isValid: Boolean = true

    override def starting(result: Result): Future[Result] = Future.successful(result)

    override val creationDate: DateTime = DateTime.lastDay
    override val user: User = self.user
    override val expirationDate: DateTime = DateTime.nextDay
    override val lastUsed: DateTime = DateTime.now
  }

}
