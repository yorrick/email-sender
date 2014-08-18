package ems.backend.utils


import scala.collection.immutable.ListMap

import securesocial.controllers.ViewTemplates
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers.{GoogleProvider, FacebookProvider}
import securesocial.core.services.{UserService, AuthenticatorService}
import scaldi.{Injector, Injectable}

import ems.controllers.EMSViewTemplates
import ems.models.User
import ems.controllers.auth.EMSRoutesService


/**
 * The runtime environment for this sample app.
 */
class EMSRuntimeEnvironment(implicit inj: Injector) extends RuntimeEnvironment.Default[User] with Injectable {
  override lazy val routes = new EMSRoutesService()

  override lazy val userService = inject[UserService[User]]
  override lazy val authenticatorService = inject[AuthenticatorService[User]]

  // override authentication views
  override lazy val viewTemplates: ViewTemplates = new EMSViewTemplates(this)

  override lazy val providers = ListMap(
    include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
    include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook)))
  )
}
