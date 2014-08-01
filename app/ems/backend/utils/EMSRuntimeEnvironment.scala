package ems.backend.utils


import scala.collection.immutable.ListMap

import securesocial.controllers.ViewTemplates
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.CookieAuthenticatorBuilder
import securesocial.core.providers.GoogleProvider
import securesocial.core.services.AuthenticatorService

import ems.backend.{MyEventListener, RedisCookieAuthenticatorStore, UserStore}
import ems.controllers.EMSViewTemplates
import ems.models.User


/**
 * The runtime environment for this sample app.
 */
class EMSRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
  override lazy val userService = UserStore

  // use AuthenticationStore based on redis (distributed)
  override lazy val authenticatorService = new AuthenticatorService(
    new CookieAuthenticatorBuilder[User](new RedisCookieAuthenticatorStore(), idGenerator)
  )

  override lazy val eventListeners = List(new MyEventListener())

  // override authentication views
  override lazy val viewTemplates: ViewTemplates = new EMSViewTemplates(this)

  override lazy val providers = ListMap(
    include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google)))
  )
}

object EMSRuntimeEnvironment {
  val instance = new EMSRuntimeEnvironment()
}
