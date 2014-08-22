package ems.backend.auth

import ems.backend.persistence.UserStore
import ems.controllers.EMSViewTemplates
import ems.controllers.auth.EMSRoutesService
import ems.models.User
import scaldi.{Injectable, Injector}
import securesocial.controllers.ViewTemplates
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers.{FacebookProvider, GoogleProvider}
import securesocial.core.services.{AuthenticatorService}

import scala.collection.immutable.ListMap


/**
 * The runtime environment for this sample app.
 */
class EMSRuntimeEnvironment(implicit inj: Injector) extends RuntimeEnvironment.Default[User] with Injectable {
  override lazy val routes = new EMSRoutesService()

  override lazy val userService = inject[UserStore]
  override lazy val authenticatorService = inject[AuthenticatorService[User]]

  // override authentication views
  override lazy val viewTemplates: ViewTemplates = new EMSViewTemplates(this)

  override lazy val providers = ListMap(
    include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
    include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook)))
  )
}
