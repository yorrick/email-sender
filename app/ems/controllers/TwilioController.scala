package ems.controllers

import akka.actor._
import ems.backend.ForwardingStore
import org.joda.time.DateTime

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Result

import ems.backend.Forwarder.forwarder
import ems.models.{SavedInMongo, Forwarding}


/**
 * Handles all requests comming from twilio
 * TODO secure this controller to ensure Twilio is making the calls!
 */
object TwilioController extends Controller {

  /**
   * Object used to build forms to validate Twilio POST requests
   */
  private[TwilioController] case class TwilioPost(from: String, to: String, content: String)


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
  def receive = Action { implicit request =>
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
    // creates a forwarding with associated user
    val forwarding = Forwarding(ForwardingStore.generateId, None, post.from, post.to, post.content, DateTime.now, SavedInMongo, "")

    forwarder ! forwarding
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
