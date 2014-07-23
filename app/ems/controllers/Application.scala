package ems.controllers

import securesocial.core._
import securesocial.core.SecureSocial

import ems.backend.DemoUser


class Application(override implicit val env: RuntimeEnvironment[DemoUser]) extends SecureSocial[DemoUser] {

  def index = UserAwareAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.index())
  }

}
