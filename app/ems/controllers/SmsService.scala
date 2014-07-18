package ems.controllers

import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}

import scala.util.{Failure, Try, Success}
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.util.Timeout
import akka.pattern
import com.github.nscala_time.time.Imports.DateTime
import reactivemongo.api._

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS, WSRequestHolder}
import play.api.mvc.Result
import play.api.libs.concurrent.Akka
import play.modules.reactivemongo.json.BSONFormats._


import ems.models._


object SmsStorage {

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("smslist")

  /**
   * Save an sms
   * @param sms
   * @return
   */
  def storeSms(sms: Sms): Future[Sms] = {
    Logger.debug(s"Storing this sms: $sms")
    val smsToInsert = sms.withStatus(SavedInMongo)

    collection.insert(smsToInsert) map {lastError => smsToInsert}
  }

  /**
   * Updates the status of an sms
   * @param sms
   */
  def updateSmsStatus(sms: Sms): Future[Sms] = {
    Logger.debug(s"Updating status of this sms: $sms")

    val modifier = BSONDocument("$set" -> BSONDocument("status.status" -> sms.status.status))
    collection.update(BSONDocument("_id" -> sms._id), modifier) map {lastError => sms}
  }

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

    responseFuture map { response =>
      sms.withStatus(SentToMailgun)
    } recover {
      case _ => sms.withStatus(NotSentToMailgun)
    }
  }
}



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
  def receive = Action.async { implicit request =>
    val smsForm = Form(
      mapping(
        "_id" -> ignored(BSONObjectID.generate),
        "From" -> text,
        "To" -> text,
        "Body" -> text,
        "creationDate" -> ignored(DateTime.now),
        "status" -> ignored(NotSavedInMongo.asInstanceOf[SmsStatus])
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
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Sms], Unit] = {
    case Success(sms) =>
      SmsUpdatesMaster.smsUpdatesMaster ! sms

  }

  private def handleFormValidated(sms: Sms): Future[Result] = {
    Logger.debug(s"Built sms object $sms")

    for {
      sms <- SmsStorage.storeSms(sms) andThen notifyWebsockets
      sms <- pattern.after(2.second, Akka.system.scheduler)(Mailgun.toEmail(sms))
      sms <- SmsStorage.updateSmsStatus(sms) andThen notifyWebsockets
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
