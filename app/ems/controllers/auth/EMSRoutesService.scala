package ems.controllers.auth

import play.api.mvc.RequestHeader
import securesocial.core.IdentityProvider
import securesocial.core.services.RoutesService


class EMSRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String =
    ems.controllers.auth.routes.LoginController.login().absoluteURL(IdentityProvider.sslEnabled)

  override def authenticationUrl(provider: String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String =
    absoluteUrl(ems.controllers.auth.routes.ProviderController.authenticate(provider, redirectTo))

}