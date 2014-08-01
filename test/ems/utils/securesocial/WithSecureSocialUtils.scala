package ems.utils.securesocial


import securesocial.core.RuntimeEnvironment
import securesocial.core.services.AuthenticatorService
import securesocial.core.authenticator._
import play.api.mvc.Cookie

import ems.models.User
import ems.backend.utils.{WithControllerUtils, EMSRuntimeEnvironment}
import ems.utils.WithMongoTestData


trait WithSecureSocialUtils extends WithControllerUtils with WithMongoTestData {

  /**
   * Cookie used for tests.
   * The value is not used as the AuthenticationStore is mocked
   * TODO find a way to use configuration (we need an app in context to be able to use CookieAuthenticator.cookieName)
   */
  lazy val cookie = Cookie("emailsenderid", "")

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