package ems.controllers

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.util.Timeout
import akka.pattern
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
import play.api.libs.concurrent.Akka
import play.api.http.Status

import ems.models._


/**
 * Handles all interactions with mongodb
 */
object SmsStorage {
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
  def storeSms(sms: Sms): Future[Sms] = {
    val smsToInsert = sms.withStatus(SavedInMongo)
    collection.insert(smsToInsert) map {lastError => smsToInsert}
  }

  /**
   * Updates the status of an sms
   * @param sms
   */
  def updateSmsStatus(sms: Sms): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("status.status" -> sms.status.status))
    update(sms, modifier)
  }

  /**
   * Set the mailgun id for an sms
   * @param sms
   */
  def setSmsMailgunId(sms: Sms): Future[Sms] = {
    val modifier = Json.obj("$set" -> Json.obj("mailgunId" -> sms.mailgunId))
    update(sms, modifier)
  }

  private def update(sms: Sms, modifier: JsObject) =
    collection.update(Json.obj("_id" -> sms._id), modifier) map {lastError => sms}

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
  def toEmail(sms: Sms, to: String = "yorrick.jansen@gmail.com"): Future[Sms] = {

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
   * @param sms
   * @param json
   * @return
   */
  private def handleMailgunResponse(sms: Sms, json: JsValue): Future[Sms] = {
    // Maigun response looks like this
    //      {
    //        "message": "Queued. Thank you.",
    //        "id": "<20140719141813.41030.12232@emsdev.mailgun.org>"
    //      }

    (json \ "id").validate[String] match {
      case JsSuccess(id, _) =>
        Logger.debug(s"Mailgun response id: $id")
        SmsStorage.setSmsMailgunId(sms.withMailgunId(id))
        Future.successful(sms.withStatus(SentToMailgun))
      case error @ JsError(_) =>
        Future.failed(new Exception(error.toString))
    }

  }
}


/**
 * Handles all http requests from Twilio, Mailgun and browsers
 */
object SmsService extends Controller {


  val emptyTwiMLResponse = """<?xml version="1.0" encoding="UTF-8"?>""" +
	<Response>
	    <Message></Message>
	</Response>

  // GET for browser
  def list = Action.async {
  	import scala.language.postfixOps
  	implicit val timeout = Timeout(1 second) 
  	
    val futureSmsList = SmsStorage.listSms().mapTo[List[Sms]]

    futureSmsList.map(smsList => Ok(ems.views.html.sms.list(smsList map {SmsDisplay.fromSms(_)}))) recover {
      case error @ _ =>
        val message = s"Could not get sms list: $error"
        Logger.warn(message)
        NotFound(message)
    }
  }

  // POST for twilio when we receive an SMS
  def twilioSms = Action.async { implicit request =>
    val smsForm = Form(
      mapping(
        "_id" -> ignored(SmsStorage.generateId),
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

  private def handleFormError(formWithErrors: Form[Sms]): Future[Result] = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    Future {
      BadRequest(message)
    }
  }

  /**
   * Hook for mailgun delivery
   * TODO secure this hook to check that mailgun is behind the request
   * @return
   */
  def mailgunSuccess = Action.async { implicit request =>
    Logger.debug(s"Request from mailgun: $request")
    Logger.debug(s"Request from mailgun headers: ${request.headers}")
    Logger.debug(s"Request from mailgun body: ${request.body}")

//    2014-07-19T23:39:01.678134+00:00 app[web.1]: [debug] application - Request from mailgun: POST /sms/api/mailgun/success
//    2014-07-19T23:39:01.678972+00:00 app[web.1]: [debug] application - Request from mailgun body: AnyContentAsFormUrlEncoded(Map(X-Mailgun-Sid -> ArrayBuffer(WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ==), domain -> ArrayBuffer(app25130478.mailgun.org), message-headers -> ArrayBuffer([["Received", "by luna.mailgun.net with HTTP; Sat, 19 Jul 2014 23:28:41 +0000"], ["Mime-Version", "1.0"], ["Content-Type", ["text/html", {"charset": "ascii"}]], ["Subject", "Sms forwarding"], ["From", "yorrick.jansen@gmail.com"], ["To", "yorrick.jansen@gmail.com"], ["Message-Id", "<20140719232841.6901.69937@app25130478.mailgun.org>"], ["Content-Transfer-Encoding", ["7bit", {}]], ["X-Mailgun-Sid", "WyI0OTljZiIsICJ5b3JyaWNrLmphbnNlbkBnbWFpbC5jb20iLCAiNTM1MGUiXQ=="], ["Date", "Sat, 19 Jul 2014 23:39:01 +0000"], ["Sender", "yorrick.jansen=gmail.com@mailgun.org"]]), Message-Id -> ArrayBuffer(<20140719232841.6901.69937@app25130478.mailgun.org>), recipient -> ArrayBuffer(yorrick.jansen@gmail.com), event -> ArrayBuffer(delivered), timestamp -> ArrayBuffer(1405813141), token -> ArrayBuffer(2hpjqvfwui2o6hsubsdvx1p2gbgbnldsa2odrrxr9rde44fu87), signature -> ArrayBuffer(7d15038989d5af7c0ef994366c2c1c55ca1e67b5a8fed18034c6255ae15a7eb8)))
//    2014-07-19T23:39:01.678538+00:00 app[web.1]: [debug] application - Request from mailgun headers: ArrayBuffer((X-Forwarded-For,ArrayBuffer(173.203.37.53)), (Connection,ArrayBuffer(close)), (Content-Length,ArrayBuffer(1216)), (X-Request-Start,ArrayBuffer(1405813141650)), (X-Forwarded-Port,ArrayBuffer(80)), (Via,ArrayBuffer(1.1 vegur)), (Total-Route-Time,ArrayBuffer(0)), (Connect-Time,ArrayBuffer(1)), (Content-Type,ArrayBuffer(application/x-www-form-urlencoded)), (X-Forwarded-Proto,ArrayBuffer(http)), (X-Request-Id,ArrayBuffer(d922ff99-9049-4b18-9940-8ec0ae705dff)), (Accept-Encoding,ArrayBuffer(gzip)), (User-Agent,ArrayBuffer(mailgun/treq-0.2.1)), (Host,ArrayBuffer(yorrick-email-sender-staging.herokuapp.com)))

    Future.successful(Ok)
  }

  /**
   * Runs all the logic after receiving a sms:
   *  - saves the sms, notify users
   *  - give he message to mailgun, notify users
   * @param sms
   * @return
   */
  private def handleFormValidated(sms: Sms): Future[Result] = {
    Logger.debug(s"Built sms object $sms")

    for {
      sms <- SmsStorage.storeSms(sms) andThen SmsUpdatesMaster.notifyWebsockets
      sms <- pattern.after(2.second, Akka.system.scheduler)(Mailgun.toEmail(sms))
      sms <- SmsStorage.updateSmsStatus(sms) andThen SmsUpdatesMaster.notifyWebsockets
    } yield Ok(emptyTwiMLResponse)

  }

  /**
   * Handles the sms updates websocket.
   */
  def updatesSocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => outActor =>
    val inActor = SmsUpdatesWebSocketActor.props(outActor, SmsUpdatesMaster.smsUpdatesMaster)
    SmsUpdatesMaster.smsUpdatesMaster ! SmsUpdatesMaster.Connect(outActor)

    inActor
  }

}
