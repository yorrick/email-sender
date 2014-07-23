package ems.controllers

import securesocial.core._
import securesocial.core.SecureSocial

import ems.backend.DemoUser


class Application(override implicit val env: RuntimeEnvironment[DemoUser]) extends SecureSocial[DemoUser] {

  def index = SecuredAction { implicit request =>
    Ok(ems.views.html.index(request.user.main))
  }

}
