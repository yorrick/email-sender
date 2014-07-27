package ems.utils.securesocial


import securesocial.core.{RuntimeEnvironment, AuthenticationMethod, BasicProfile}
import securesocial.core.services.AuthenticatorService
import securesocial.core.authenticator._
import play.api.mvc.Cookie

import ems.models.User
import ems.backend.utils.{WithControllerUtils, EMSRuntimeEnvironment}


trait WithSecureSocialUtils extends WithControllerUtils {

  /**
   * Cookie used for tests.
   * The value is not used as the AuthenticationStore is mocked
   * TODO find a way to use configuration (we need an app in context to be able to use CookieAuthenticator.cookieName)
   */
  val cookie = Cookie("emailsenderid", "")

  /**
   * The user the tests will be based on
   * @return
   */
  lazy val user: User = User(BasicProfile(
    "providerId",
    "userid-12345",
    Some("Paul"),
    Some("Watson"),
    Some("Paul Watson"),
    Some("paul.watson@foobar.com"),
    None,
    AuthenticationMethod.OAuth2,
    None,
    None,
    None
  ))

  /**
   * The runtime environment that will be injected to the classes
   */
  lazy val runtimeEnvironment = new EMSRuntimeEnvironment {
    override lazy val authenticatorService = new AuthenticatorService(
      new CookieAuthenticatorBuilder[User](new MockAuthenticatorStore(user), idGenerator)
    )
  }

  /**
   * Creates a controller for the given class
   * @param controllerClass
   * @tparam A
   * @return
   */
  def createController[A](controllerClass: Class[A], env: RuntimeEnvironment[User] = runtimeEnvironment) = getControllerInstance[A, User](env)(controllerClass).get

}