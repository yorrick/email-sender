package controllers

import play.api.mvc.WebSocket.FrameFormatter

import scala.collection.mutable
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
import play.api.libs.concurrent.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

import controllers.SmsUpdatesMaster.{Broadcast, Disconnect, Connect}
import models.{JsonFormats, Sms}


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

  // create the master actor once
  val smsUpdatesMaster = Akka.system.actorOf(Props[SmsUpdatesMaster], name="smsUpdatesMaster")

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
        // send notification
        smsUpdatesMaster ! Broadcast(sms)
        Ok(emptyTwiMLResponse)
      }
    }

  }

  import JsonFormats.smsFormat
  implicit val smsFrameFormatter = FrameFormatter.jsonFrame[Sms]

  /**
   * Handles the sms updates websocket.
   */
  def updatesSocket = WebSocket.acceptWithActor[JsValue, Sms] { request => outActor =>
    val inActor = SmsUpdatesWebSocketActor.props(outActor, smsUpdatesMaster)
    smsUpdatesMaster ! SmsUpdatesMaster.Connect(outActor)

    inActor
  }

  def updatesJs() = Action { implicit request =>
    Ok(views.js.sms.updates())
  }

}


object SmsUpdatesMaster {
  case class Connect(val outActor: ActorRef)
  case class Disconnect(val outActor: ActorRef)
  case class Broadcast(val sms: Sms)
}


class SmsUpdatesMaster extends Actor {
  private val webSocketOutActors = mutable.ListBuffer[ActorRef]()

  def receive = {
    case Connect(actor) =>
      Logger.debug("Opened a websocket connection")
      webSocketOutActors += actor
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")
    case Disconnect(actor) =>
      Logger.debug("Websocket connection has closed")
      webSocketOutActors -= actor
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")
    case Broadcast(sms) =>
      Logger.debug(s"Broadcast sms $sms")
      webSocketOutActors foreach {outActor => outActor ! sms}
  }
}


object SmsUpdatesWebSocketActor {
  def props(out: ActorRef, master: ActorRef) = Props(new SmsUpdatesWebSocketActor(out, master))
}

class SmsUpdatesWebSocketActor(val outActor: ActorRef, val master: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      outActor ! ("I received your message: " + msg)
  }

  override def postStop() = {
    master ! SmsUpdatesMaster.Disconnect(outActor)
  }
}
