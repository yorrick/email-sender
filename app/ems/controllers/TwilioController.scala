package ems.controllers

import akka.actor._
import ems.backend.mongo.MongoDBUtils
import org.joda.time.DateTime

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Result
import scaldi.akka.AkkaInjectable
import scaldi.Injector

import ems.models.{Received, Forwarding}
import ems.backend.{Forwarder, ForwardingStore}


/**
 * Handles all requests comming from twilio
 * TODO secure this controller to ensure Twilio is making the calls!
 */
class TwilioController(implicit inj: Injector) extends Controller with AkkaInjectable with MongoDBUtils {

  implicit val system = inject[ActorSystem]
  val forwarder = injectActorRef[Forwarder]

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
    // creates a forwarding with no associated user and destination
    val forwarding = Forwarding(generateId, None, post.from, None, post.content, DateTime.now, Received, "")

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
