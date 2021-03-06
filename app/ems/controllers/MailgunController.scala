package ems.controllers

import akka.actor.{ActorRef, ActorSystem}
import org.joda.time.DateTime
import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Result
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import ems.models
import ems.backend.forwarding.ForwarderServiceActor
import ems.backend.persistence.mongo.MongoDBUtils
import ems.models.{Received, Sent, Failed, Message}


/**
 * Handles all requests comming from mailgun
 * TODO secure this controller to ensure mailgun is making the calls!
 */
class MailgunController(implicit inj: Injector) extends Controller with AkkaInjectable with MongoDBUtils {

  implicit val system = inject[ActorSystem]
  val forwarder: ActorRef = injectActorRef[ForwarderServiceActor]
  val delivered = inject[String] (identified by "ems.controllers.MailgunController.delivered")

  /**
   * Object used to build forms to validate Mailgun POST requests for email deliveries
   */
  private[MailgunController] case class MailgunEvent(messageId: String, event: String)
  val eventForm = Form(mapping("Message-Id" -> text, "event" -> text)(MailgunEvent.apply)(MailgunEvent.unapply))

  /**
   * Object used to build forms to validate Mailgun POST requests for email receiving
   */
  private[MailgunController] case class MailgunReceive(from: String, to: String, subject: String, content: String)
  val receiveForm = Form(
    mapping("from" -> text, "recipient" -> text, "subject" -> text, "body-plain" -> text
  )(MailgunReceive.apply)(MailgunReceive.unapply))

  val emailRegex = """.*<(.*)>""".r

  /**
   * Hook for mailgun email receiving
   * @return
   */
  def receive = Action { implicit request =>
    Logger.debug(s"Raw data for email: ${request.body.asFormUrlEncoded}")

    receiveForm.bindFromRequest.fold(
      formWithErrors => errorReceiveForm(formWithErrors),
      receive => validatedReceiveForm(receive)
    )
  }

  /**
   * Notifies the forwarder that a message has arrived
   * Replies with Ok as fast as possible
   * @param receive
   * @return
   */
  private def validatedReceiveForm(receive: MailgunReceive): Result = {
    extractEmail(receive.from) match {
      case Some(from) =>
        Logger.debug(s"Extracted email '$from' from string '${receive.from}'")

        // creates a message with no associated user and no destination
        val message = Message(generateId, None, from, None,
          extractContent(receive.subject, receive.content), DateTime.now, Received, "")

        forwarder ! message
      case None =>
        Logger.debug(s"Could mot extract email from string '${receive.from}'")
    }

    Ok
  }

  /**
   * Just return a BadRequest
   * @param formWithErrors
   * @return
   */
  private def errorReceiveForm(formWithErrors: Form[MailgunReceive]): Result = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    BadRequest(message)
  }

  /**
   * Extract the user email from the raw string "Somebody <somebody@example.com>"
   *
   */
  def extractEmail(rawFrom: String): Option[String] =
    emailRegex.findFirstMatchIn(rawFrom) map { _.group(1)}

  /**
   * Extract the content from the subject and the multiline raw email content
   * @param subject
   * @param rawContent
   * @return
   */
  def extractContent(subject: String, rawContent: String): String = {
    val stripedContent = rawContent.replaceAll(">.*", "").replaceAll("""\d{4}-.*""", "").replaceAll("(?m)^[ \t]*\r?\n", "")
    stripedContent.stripLineEnd
  }

  /**
   * Hook for mailgun delivery
   *
   * AnyContentAsFormUrlEncoded(
   *   Map(
   *     X-Mailgun-Sid -> ArrayBuffer(WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ==),
   *     domain -> ArrayBuffer(xxxxxx.mailgun.org),
   *     message-headers -> ArrayBuffer(["Received", "by luna.mailgun.net with HTTP; Sat, 19 Jul 2014 23:28:41 +0000"], ["Mime-Version", "1.0"], ["Content-Type", ["text/html", {"charset": "ascii"}]], ["Subject", "Forwarding"], ["From", "yorrick.jansen@gmail.com"], ["To", "yorrick.jansen@gmail.com"], ["Message-Id", "<20140719232841.6901.69937@app25130478.mailgun.org>"], ["Content-Transfer-Encoding", ["7bit", {}]], ["X-Mailgun-Sid", "WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ=="], ["Date", "Sat, 19 Jul 2014 23:39:01 +0000"], ["Sender", "yorrick.jansen=gmail.com@mailgun.org"]),
   *     Message-Id -> ArrayBuffer(<20140719232841.6901.69937@xxxxxx.mailgun.org>),
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
  def event = Action { implicit request =>
    eventForm.bindFromRequest.fold(
      formWithErrors => errorEventForm(formWithErrors),
      event => validatedEventForm(event)
    )
  }

  /**
   * Notifies the forwarder that a MailgunEvent has arrived
   * Replies with Ok as fast as possible
   * @param event
   * @return
   */
  private def validatedEventForm(event: MailgunEvent): Result = {
    val status = if (event.event == delivered) Sent else Failed
    forwarder ! models.MailgunEvent(event.messageId, status)

    Ok
  }

  /**
   * Just return a BadRequest
   * @param formWithErrors
   * @return
   */
  private def errorEventForm(formWithErrors: Form[MailgunEvent]): Result = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    BadRequest(message)
  }


}
