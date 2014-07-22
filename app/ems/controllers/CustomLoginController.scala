package ems.controllers

import securesocial.controllers.BaseLoginPage
import securesocial.core.{RuntimeEnvironment, IdentityProvider}
import securesocial.core.services.RoutesService

import play.api.mvc.{RequestHeader, AnyContent, Action}
import play.api.Logger

import ems.backend.DemoUser


class CustomLoginController(implicit override val env: RuntimeEnvironment[DemoUser]) extends BaseLoginPage[DemoUser] {
  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }
}


class CustomRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String =
    ems.controllers.routes.CustomLoginController.login().absoluteURL(IdentityProvider.sslEnabled)
}
