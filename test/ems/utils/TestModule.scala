package ems.utils

import _root_.securesocial.core.authenticator.{IdGenerator, CookieAuthenticatorBuilder}
import _root_.securesocial.core.services.AuthenticatorService
import ems.models.User
import ems.utils.securesocial.MockAuthenticatorStore
import scaldi.Module
import scaldi.play.condition._


class TestModule extends Module {

  // use mock AuthenticatorService in test
  bind[AuthenticatorService[User]] when inTestMode to new AuthenticatorService(
    new CookieAuthenticatorBuilder[User](new MockAuthenticatorStore, new IdGenerator.Default())
  )

}
