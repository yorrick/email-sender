package ems.controllers

import controllers.Assets
import scaldi.{Injectable, Injector}
import securesocial.core._
import securesocial.core.SecureSocial
import play.api.Logger

import ems.models.User


class MainController(implicit inj: Injector) extends SecureSocial[User] with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]

  def index = UserAwareAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.index())
  }

}
