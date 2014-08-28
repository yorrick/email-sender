package ems.controllers

import play.api.Routes
import play.api.mvc.{Action, Controller}
import scaldi.{Injectable, Injector}


class JavascriptController(implicit inj: Injector) extends Controller with Injectable {

  def updatesJs() = Action { implicit request =>
    Ok(ems.views.js.message.updates())
  }

  def javascriptRoutes() = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.MessageController.updatesSocket
      )
    ).as("text/javascript")
  }

}
