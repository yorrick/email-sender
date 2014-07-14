package controllers

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.util.Timeout
import com.github.nscala_time.time.Imports.DateTime
import reactivemongo.core.commands.LastError
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

import models.{SmsDisplay, Sms}


object SmsStorage {

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("smslist")

  def storeSms(sms: Sms): Future[LastError] = {
    Logger.debug(s"Storing this sms: $sms")
    collection.insert(sms)
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

  def requestHolder: Option[WSRequestHolder] = for {
    key <- key
    apiUrl <- apiUrl
  } yield WS.url(apiUrl).withAuth("api", key, WSAuthScheme.BASIC)

  def toEmail(sms: Sms, to: String) = {
    val postData = Map(
      "from" -> Seq(to),
      "to" -> Seq(to),
      "subject" -> Seq("Sms forwarding"),
      "html" -> Seq(sms.content)
    )

    val future: Future[WSResponse] = requestHolder.map(_.post(postData)).getOrElse(Future.failed(missingCredentials))

    future onComplete {
      case Success(response: WSResponse) =>
        Logger.debug(s"Response status: ${response.status}")
        Logger.debug(s"Response status text ${response.statusText}")
      case Failure(t) =>
        Logger.warn("Could not send email: " + t.getMessage)
    }

    future
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
        "From" -> text,
        "To" -> text,
        "Body" -> text,
        "creationDate" -> ignored(DateTime.now)
      )(Sms.apply)(Sms.unapply))

    smsForm.bindFromRequest.fold(
      formWithErrors => handleFormError(formWithErrors),
      sms => handleFormValidated(sms)
    )
  }

  private def handleFormError(formWithErrors: Form[Sms]) = {
    val message = s"Could not bind the form: ${formWithErrors}"
    Logger.warn(message)
    Future {
      BadRequest(message)
    }
  }

  private def handleFormValidated(sms: Sms) = {
    Logger.debug(s"Built sms object $sms")
    SmsStorage.storeSms(sms).mapTo[LastError] map { lastError =>
      if (lastError.inError == true) {
        val message = s"Could not save the sms: ${lastError.message}"
        Logger.warn(message)
        BadRequest(message)
      } else {
        SmsUpdatesMaster.smsUpdatesMaster ! sms
        Mailgun.toEmail(sms, "yorrick.jansen@gmail.com")

        Ok(emptyTwiMLResponse)
      }
    }

  }

  /**
   * Handles the sms updates websocket.
   */
  def updatesSocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => outActor =>
    val inActor = SmsUpdatesWebSocketActor.props(outActor, SmsUpdatesMaster.smsUpdatesMaster)
    SmsUpdatesMaster.smsUpdatesMaster ! SmsUpdatesMaster.Connect(outActor)

    inActor
  }

  def updatesJs() = Action { implicit request =>
    Ok(ems.views.js.sms.updates())
  }

}
