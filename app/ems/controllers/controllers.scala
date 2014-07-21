package ems.controllers

import scala.concurrent.duration._
import scala.concurrent.Future

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
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS, WSRequestHolder}
import play.api.mvc.Result
import play.api.http.Status

import ems.models._


/**
 * Handles all interactions with mongodb
 */
object MongoDB {
  import reactivemongo.api._
  import reactivemongo.bson.BSONObjectID
  import play.modules.reactivemongo.json.BSONFormats._
  import play.modules.reactivemongo.ReactiveMongoPlugin
  import play.modules.reactivemongo.json.collection.JSONCollection

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("smslist")
  def generateId = BSONObjectID.generate

  /**
   * Save an sms
   * @param sms
   * @return
   */
  def save(sms: Sms): Future[Sms] = {
    val smsToInsert = sms.withStatus(SavedInMongo)
    collection.insert(smsToInsert) map {lastError => smsToInsert}
  }

  /**
   * Updates the status of an sms using the id
   * @param sms
   */
  def updateStatusById(sms: Sms): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> sms.status.status))
    updateById(sms, modifier)
  }

  /**
   * Ack a sms, given a mailgunId
   * Returns the acked sms
   * @param mailgunId
   */
  def setStatusByMailgunId(mailgunId: String, status: SmsStatus): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> status.status))
    val findId = Json.obj("mailgunId" -> mailgunId)

    collection.update(findId, modifier) flatMap { lastError =>
      val cursor = collection.find(findId).cursor[Sms]
      // return the first result
      cursor.collect[List]() map { _.head }
    }
  }

  /**
   * Set the mailgun id for an sms
   * @param sms
   */
  def setSmsMailgunId(sms: Sms): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> sms.mailgunId))
    updateById(sms, modifier)
  }

  private def updateById(sms: Sms, modifier: JsObject) =
    collection.update(Json.obj("_id" -> sms._id), modifier) map {lastError => sms}

  /**
   * Returns the list of sms
   * @return
   */
  def listSms(): Future[List[Sms]] = {
    // let's do our query
    val cursor: Cursor[Sms] = collection.
      // find all sms
      find(Json.obj()).
      // sort by creation date
      sort(Json.obj("creationDate" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[Sms]

    // gather all the JsObjects in a list
    cursor.collect[List]()
  }

}


/**
 * Utility that send emails using mailgun http api.
 */
object Mailgun {

  val key = current.configuration.getString("mailgun.api.key")
  // we retrieve the domain from the smtp login since heroku mailgun does not give the domain alone
  val domain = current.configuration.getString("mailgun.smtp.login").map(_.split("@").last)
  val apiUrl = domain map {domain => s"https://api.mailgun.net/v2/${domain}/messages"}
  lazy val missingCredentials = new Exception(s"Missing credentials: key ($key) or domain ($domain)")

  val DELIVERED = "delivered"

  def requestHolderOption: Option[WSRequestHolder] = for {
    key <- key
    apiUrl <- apiUrl
  } yield WS.url(apiUrl).withAuth("api", key, WSAuthScheme.BASIC)

  /**
   * Mailgun call will never reply with a failure, but with an sms that contains the updated status
   * @param sms
   * @param to
   * @return
   */
  def sendEmail(sms: Sms, to: String = "yorrick.jansen@gmail.com"): Future[Sms] = {

    val postData = Map(
      "from" -> Seq(to),
      "to" -> Seq(to),
      "subject" -> Seq("Sms forwarding"),
      "html" -> Seq(sms.content)
    )

    val responseFuture: Future[WSResponse] = requestHolderOption map { requestHolder =>
      requestHolder.post(postData)
    } getOrElse Future.failed(missingCredentials)

    val okResponse = responseFuture filter { _.status == Status.OK }
    val smsResponse = okResponse flatMap { response => handleMailgunResponse(sms, response.json)}

    // in case something went wrong
    smsResponse recover {
      case _ => sms.withStatus(NotSentToMailgun)
    }
  }

  /**
   * Returns the given sms with updated status if everything went fine
   *
   * Mailgun response looks like this
   *       {
   *         "message": "Queued. Thank you.",
   *         "id": "<20140719141813.41030.12232@emsdev.mailgun.org>"
   *       }
   *
   * @param sms
   * @param json
   * @return
   */
  private def handleMailgunResponse(sms: Sms, json: JsValue): Future[Sms] = {
    (json \ "id").validate[String] match {
      case JsSuccess(id, _) =>
        Logger.debug(s"Mailgun response id: $id")
        MongoDB.setSmsMailgunId(sms.withMailgunId(id))
        Future.successful(sms.withStatus(SentToMailgun))
      case error @ JsError(_) =>
        Future.failed(new Exception(error.toString))
    }

  }
}


/**
 * Handles all requests comming from twilio
 * TODO secure this controller to ensure Twilio is making the calls!
 * TODO create a special case class for post, and a conversion to create Sms objects
 */
object TwilioController extends Controller {
  import ems.controllers.SmsForwarder.smsForwarder

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


/**
 * Handles all requests comming from mailgun
 * TODO secure this controller to ensure mailgun is making the calls!
 * TODO separate mailgun and routing concerns
 */
object MailgunController extends Controller {
  import ems.models.MailgunEvent
  import ems.controllers.SmsForwarder.smsForwarder

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
   *     message-headers -> ArrayBuffer([["Received", "by luna.mailgun.net with HTTP; Sat, 19 Jul 2014 23:28:41 +0000"], ["Mime-Version", "1.0"], ["Content-Type", ["text/html", {"charset": "ascii"}]], ["Subject", "Sms forwarding"], ["From", "yorrick.jansen@gmail.com"], ["To", "yorrick.jansen@gmail.com"], ["Message-Id", "<20140719232841.6901.69937@app25130478.mailgun.org>"], ["Content-Transfer-Encoding", ["7bit", {}]], ["X-Mailgun-Sid", "WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ=="], ["Date", "Sat, 19 Jul 2014 23:39:01 +0000"], ["Sender", "yorrick.jansen=gmail.com@mailgun.org"]]),
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


/**
 * Handles all http requests from browsers
 */
object SmsController extends Controller {

  /**
   * GET for browser
   * @return
   */
  def list = Action.async {
  	import scala.language.postfixOps
  	implicit val timeout = Timeout(1 second)

    val futureSmsList = MongoDB.listSms().mapTo[List[Sms]]

    futureSmsList.map(smsList => Ok(ems.views.html.sms.list(smsList map {SmsDisplay.fromSms(_)}))) recover {
      case error @ _ =>
        val message = s"Could not get sms list: $error"
        Logger.warn(message)
        NotFound(message)
    }
  }

  /**
   * Initiate the websocket connection
   */
  def updatesSocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => outActor =>
    WebsocketInputActor(outActor)
  }

}
