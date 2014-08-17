package ems.controllers

import ems.models.User
import play.api.mvc.{Controller, Action}
import scaldi.{Injector, Injectable}
import securesocial.core.RuntimeEnvironment


class InjectedApplication(implicit inj: Injector) extends Controller with Injectable {

  implicit val env = inject [RuntimeEnvironment[User]]

  val message = inject [String] (identified by "my-message")

  def index = Action { implicit request =>
    println(s"Message: $message")

    implicit val user = None
    Ok(ems.views.html.index())
  }

}
