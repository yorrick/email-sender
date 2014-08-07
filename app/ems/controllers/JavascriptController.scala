package ems.controllers

import play.api.Routes
import play.api.mvc.{Action, Controller}


object JavascriptController extends Controller {

  def updatesJs() = Action { implicit request =>
    Ok(ems.views.js.forwarding.updates())
  }

  def javascriptRoutes() = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.ForwardingController.updatesSocket
      )
    ).as("text/javascript")
  }

}
