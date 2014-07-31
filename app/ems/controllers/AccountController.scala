package ems.controllers

import ems.models.User
import play.api.data.Form
import play.api.data.Forms._
import securesocial.core.{RuntimeEnvironment, SecureSocial}


/**
 * Handles authentication custom views
 */
class AccountController(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {

  /**
   * Account view
   * @return
   */
  def account = SecuredAction { implicit request =>
    implicit val user = request.user
    Ok(ems.views.html.auth.account())
  }

//  val eventForm = Form(mapping("Message-Id" -> text, "event" -> text)(MailgunEvent.apply)(MailgunEvent.unapply))

  def phoneNumber = SecuredAction { implicit request =>

    Ok
  }

}
