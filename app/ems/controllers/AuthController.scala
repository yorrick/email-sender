package ems.controllers

import ems.models.User
import securesocial.core.{RuntimeEnvironment, SecureSocial}


/**
 * Handles authentication custom views
 */
class AuthController(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {

  /**
   * Account view
   * @return
   */
  def account = SecuredAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.auth.account())
  }

}
