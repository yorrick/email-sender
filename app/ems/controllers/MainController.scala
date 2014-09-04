package ems.controllers

import ems.controllers.utils.ControllerUtils
import io.prismic.Fragment.WebLink
import scaldi.{Injectable, Injector}
import securesocial.core._
import ems.models.User


class MainController(implicit val injector: Injector) extends ControllerUtils with Injectable {

  override implicit val env = inject[RuntimeEnvironment[User]]

  def index = userAwareContextAction("welcome") { implicit user => implicit ctx => implicit request =>
    Ok(ems.views.html.index())
  }

}
