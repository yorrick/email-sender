package ems.controllers

import ems.models.User
import securesocial.core._
import securesocial.core.SecureSocial


class Application(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {

  def index = UserAwareAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.index())
  }

}
