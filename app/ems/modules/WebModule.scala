package ems.modules

import scaldi.Module
import scaldi.play.condition._
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.{CookieAuthenticator, AuthenticatorStore, IdGenerator, CookieAuthenticatorBuilder}
import securesocial.core.services.{AuthenticatorService, UserService}

import ems.backend.utils.EMSRuntimeEnvironment
import ems.models.User
import ems.backend.{RedisCookieAuthenticatorStore, UserStore}


class WebModule extends Module {
  bind identifiedBy "my-message" to "web module message"

  bind[RuntimeEnvironment[User]] to new EMSRuntimeEnvironment

  bind[UserService[User]] to UserStore

  // use real AuthenticatorService in dev and prod
  bind[AuthenticatorService[User]] when (inDevMode or inProdMode) to new AuthenticatorService(
    new CookieAuthenticatorBuilder[User](new RedisCookieAuthenticatorStore(), new IdGenerator.Default())
  )

  bind[AuthenticatorStore[CookieAuthenticator[User]]] to new RedisCookieAuthenticatorStore()

}
