package ems.controllers

import scaldi.{Injectable, Injector}
import securesocial.core._
import securesocial.core.SecureSocial

import ems.models.User


class Application(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {
//class Application(implicit inj: Injector) extends SecureSocial[User] with Injectable {

//  override implicit val env = inject [RuntimeEnvironment[User]]

  def index = UserAwareAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.index())
  }

}
