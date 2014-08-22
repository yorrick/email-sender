package ems.controllers.auth

import ems.models.User
import scaldi.{Injectable, Injector}
import securesocial.controllers.BaseLoginPage
import securesocial.core.RuntimeEnvironment


/**
 * Overrides the login page to integrate this controller into scaldi DI
 * @param inj
 */
class LoginController(implicit inj: Injector) extends BaseLoginPage[User] with Injectable {
  override implicit val env = inject [RuntimeEnvironment[User]]
}
