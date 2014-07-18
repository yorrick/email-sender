package ems.controllers

import play.api.mvc.{Action, Controller}


object Application extends Controller {

  def index = Action {
    Ok(ems.views.html.index("Send emails using SMS"))
  }

}