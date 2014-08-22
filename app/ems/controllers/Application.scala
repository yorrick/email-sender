package ems.controllers

import scaldi.{Injectable, Injector}
import securesocial.core._
import securesocial.core.SecureSocial
import play.api.Logger

import ems.models.User


class Application(implicit inj: Injector) extends SecureSocial[User] with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]
//  val message = inject [String] (identified by "my-message")

  def index = UserAwareAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.index())
  }

}
