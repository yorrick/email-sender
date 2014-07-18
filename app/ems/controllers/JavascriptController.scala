package ems.controllers

import play.api.mvc.{Action, Controller}


object JavascriptController extends Controller {

  def updatesJs() = Action { implicit request =>
    Ok(ems.views.js.sms.updates())
  }

}
