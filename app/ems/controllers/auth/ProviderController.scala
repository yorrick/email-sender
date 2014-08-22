package ems.controllers.auth

import ems.models.User
import scaldi.{Injectable, Injector}
import securesocial.controllers.{BaseProviderController, BaseLoginPage}
import securesocial.core.{BasicProfile, RuntimeEnvironment}


/**
 * Overrides the provider page to integrate this controller into scaldi DI
 * @param inj
 */
class ProviderController(implicit inj: Injector) extends BaseProviderController[User] with Injectable {
  override implicit val env = inject [RuntimeEnvironment[User]]
}
