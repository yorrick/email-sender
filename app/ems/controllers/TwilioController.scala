package ems.controllers

import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import com.github.nscala_time.time.Imports.DateTime

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Result

import ems.backend.MongoDB
import ems.backend.SmsForwarder.smsForwarder
import ems.backend.WebsocketInputActor
import ems.models._


/**
 * Handles all requests comming from twilio
 * TODO secure this controller to ensure Twilio is making the calls!
 * TODO create a special case class for post, and a conversion to create Sms objects
 */
object TwilioController extends Controller {

  val emptyTwiMLResponse = """<?xml version="1.0" encoding="UTF-8"?>""" +
    <Response>
      <Message></Message>
    </Response>

  /**
   * Object used to build forms to validate Twilio POST requests
   */
  //  private case class TwilioSms(from: String, to: String, content: String)

  /**
   * POST for twilio when we receive an SMS
   * @return
   */
  def sms = Action { implicit request =>
    val smsForm = Form(
      mapping(
        "_id" -> ignored(MongoDB.generateId),
        "From" -> text,
        "To" -> text,
        "Body" -> text,
        "creationDate" -> ignored(DateTime.now),
        "status" -> ignored(NotSavedInMongo.asInstanceOf[SmsStatus]),
        "mailgunId" -> ignored("")
      )(Sms.apply)(Sms.unapply))

    smsForm.bindFromRequest.fold(
      formWithErrors => handleFormError(formWithErrors),
      sms => handleFormValidated(sms)
    )
  }

  /**
   * Notifies the forwarder that a sms has arrived
   * Replies with Ok as fast as possible
   * @param sms
   * @return
   */
  private def handleFormValidated(sms: Sms): Result = {
    smsForwarder ! sms
    Ok(emptyTwiMLResponse)
  }

  /**
   * Just return a BadRequest with a message
   * @param formWithErrors
   * @return
   */
  private def handleFormError(formWithErrors: Form[Sms]): Result = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    BadRequest(message)
  }

}
