package controllers

import akka.actor.{Actor, ActorRef, Props}
import models.{JsonFormats, Sms}
import reactivemongo.core.commands.LastError
import com.github.nscala_time.time.Imports.DateTime

import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.api.libs.json._

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

import akka.util.Timeout

import reactivemongo.api._


object SmsStorage {

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("smslist")

  def storeSms(sms: Sms): Future[LastError] = {
    import JsonFormats._
    Logger.debug(s"Storing this sms: $sms")
    collection.insert(sms)
  }

  def listSms(): Future[List[Sms]] = {
    import JsonFormats._
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


object SmsService extends Controller {

  val smsForm = Form(
	  mapping(
	    "From" -> text,
	    "To" -> text,
	    "Body" -> text,
      "creationDate" -> ignored(DateTime.now)
	  )(Sms.apply)(Sms.unapply))

  val emptyTwiMLResponse = """<?xml version="1.0" encoding="UTF-8"?>""" + 
	<Response>
	    <Message></Message>
	</Response>

  // GET for browser
  def list = Action.async {
  	import scala.language.postfixOps
  	implicit val timeout = Timeout(1 second) 
  	
    val futureSmsList = SmsStorage.listSms().mapTo[List[Sms]]

    futureSmsList.map(smsList => Ok(views.html.sms.list(smsList))) recover {
      case error @ _ =>
        val message = s"Could not get sms list: $error"
        Logger.warn(message)
        NotFound(message)
    }
  }

  // POST for twilio when we receive an SMS
  def receive = Action.async { implicit request =>

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
        Ok(emptyTwiMLResponse)
      }
    }
  }

  /**
   * Handles the sms updates websocket.
   */
  def updatesSocket = WebSocket.acceptWithActor[String, String] { request => out =>
    SmsUpdatesWebSocketActor.props(out)
  }

  def updatesJs() = Action { implicit request =>
    Ok(views.js.sms.updates())
  }

}

object SmsUpdatesWebSocketActor {
  def props(out: ActorRef) = Props(new SmsUpdatesWebSocketActor(out))
}

class SmsUpdatesWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }
}