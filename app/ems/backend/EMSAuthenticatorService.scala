package ems.backend


import scaldi.{Injector, Injectable}
import securesocial.core.authenticator.{AuthenticatorStore, CookieAuthenticator, IdGenerator, CookieAuthenticatorBuilder}
import securesocial.core.services.AuthenticatorService

import ems.models.User


/**
 * Autenticator service for the project
 * It only uses Cookies
 */
class EMSAuthenticatorService(implicit inj: Injector) extends AuthenticatorService[User] with Injectable {
  val store = inject[AuthenticatorStore[CookieAuthenticator[User]]]
  val builders = new CookieAuthenticatorBuilder[User](store, new IdGenerator.Default())
}