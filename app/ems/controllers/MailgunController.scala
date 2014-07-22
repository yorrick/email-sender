package ems.controllers

import akka.actor._

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Result

import ems.backend.SmsForwarder.smsForwarder
import ems.models._


/**
 * Handles all requests comming from mailgun
 * TODO secure this controller to ensure mailgun is making the calls!
 */
object MailgunController extends Controller {

  /**
   * Validation form
   */
  val eventForm = Form(mapping("Message-Id" -> text, "event" -> text)(MailgunEvent.apply)(MailgunEvent.unapply))

  /**
   * Hook for mailgun delivery
   *
   * AnyContentAsFormUrlEncoded(
   *   Map(
   *     X-Mailgun-Sid -> ArrayBuffer(WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ==),
   *     domain -> ArrayBuffer(app25130478.mailgun.org),
   *     message-headers -> ArrayBuffer(["Received", "by luna.mailgun.net with HTTP; Sat, 19 Jul 2014 23:28:41 +0000"], ["Mime-Version", "1.0"], ["Content-Type", ["text/html", {"charset": "ascii"}]], ["Subject", "Sms forwarding"], ["From", "yorrick.jansen@gmail.com"], ["To", "yorrick.jansen@gmail.com"], ["Message-Id", "<20140719232841.6901.69937@app25130478.mailgun.org>"], ["Content-Transfer-Encoding", ["7bit", {}]], ["X-Mailgun-Sid", "WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ=="], ["Date", "Sat, 19 Jul 2014 23:39:01 +0000"], ["Sender", "yorrick.jansen=gmail.com@mailgun.org"]),
   *     Message-Id -> ArrayBuffer(<20140719232841.6901.69937@app25130478.mailgun.org>),
   *     recipient -> ArrayBuffer(yorrick.jansen@gmail.com),
   *     event -> ArrayBuffer(delivered),
   *     timestamp -> ArrayBuffer(1405813141),
   *     token -> ArrayBuffer(2hpjqvfwui2o6hsubsdvx1p2gbgbnldsa2odrrxr9rde44fu87),
   *     signature -> ArrayBuffer(7d15038989d5af7c0ef994366c2c1c55ca1e67b5a8fed18034c6255ae15a7eb8)
   *   )
   * )
   *
   * @return
   */
  def success = Action { implicit request =>
    eventForm.bindFromRequest.fold(
      formWithErrors => handleFormError(formWithErrors),
      sms => handleFormValidated(sms)
    )
  }

  /**
   * Notifies the forwarder that a MailgunEvent has arrived
   * Replies with Ok as fast as possible
   * @param event
   * @return
   */
  private def handleFormValidated(event: MailgunEvent): Result = {
    smsForwarder ! event
    Ok
  }

  /**
   * Just return a BadRequest
   * @param formWithErrors
   * @return
   */
  private def handleFormError(formWithErrors: Form[MailgunEvent]): Result = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    BadRequest(message)
  }


}