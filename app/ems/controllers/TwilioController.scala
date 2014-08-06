package ems.controllers

import akka.actor._

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Result

import ems.backend.SmsForwarder.smsForwarder
import ems.models.TwilioPost


/**
 * Handles all requests comming from twilio
 * TODO secure this controller to ensure Twilio is making the calls!
 */
object TwilioController extends Controller {

  val form = Form(
    mapping(
      "From" -> text,
      "To" -> text,
      "Body" -> text
    )(TwilioPost.apply)(TwilioPost.unapply))

  /**
   * POST for twilio when we receive an SMS
   * @return
   */
  def sms = Action { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => handleFormError(formWithErrors),
      post => handleFormValidated(post)
    )
  }

  /**
   * Notifies the forwarder that a sms has arrived
   * Replies with Ok as fast as possible
   * @param post
   * @return
   */
  private def handleFormValidated(post: TwilioPost): Result = {
    smsForwarder ! post
    Ok
  }

  /**
   * Just return a BadRequest with a message
   * @param formWithErrors
   * @return
   */
  private def handleFormError(formWithErrors: Form[TwilioPost]): Result = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    BadRequest(message)
  }

}
