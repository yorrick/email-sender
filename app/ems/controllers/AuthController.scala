package ems.controllers

import securesocial.core.{RuntimeEnvironment, SecureSocial}

import ems.backend.DemoUser


/**
 * Handles authentication custom views
 */
class AuthController(override implicit val env: RuntimeEnvironment[DemoUser]) extends SecureSocial[DemoUser] {

  /**
   * Account view
   * @return
   */
  def account = SecuredAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.auth.account())
  }

}
