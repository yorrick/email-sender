package controllers

import java.nio.charset.Charset

import akka.camel.{CamelMessage, Consumer, Oneway, Producer}

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

import controllers.SmsUpdatesMaster.{Disconnect, Connect}
import models._


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

    futureSmsList.map(smsList => Ok(views.html.sms.list(smsList map {SmsDisplay.fromSms(_)}))) recover {
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
    Ok(views.js.sms.updates())
  }

}


object SmsUpdatesMaster {

  /**
   * Message that must be sent to web browsers clients.
   */
  case class Connect(val outActor: ActorRef)
  case class Disconnect(val outActor: ActorRef)

  // create the master actor once
  val smsUpdatesMaster = Akka.system.actorOf(Props[SmsUpdatesMaster], name="smsUpdatesMaster")

  val dispatcherName = "rabbitmq-dispatcher"
  val rabbitMQProducer = Akka.system.actorOf(Props[RabbitMQProducer].withDispatcher(dispatcherName))
  val rabbitMQConsumer = Akka.system.actorOf(Props(classOf[RabbitMQConsumer], smsUpdatesMaster).withDispatcher(dispatcherName))

  // we periodically ping the client so the websocket connections do not close
  Akka.system.scheduler.schedule(30.second, 30.second, smsUpdatesMaster, Ping)
}


class RabbitMQProducer extends Producer with Oneway {
  def endpointUri = "rabbitmq://localhost/testExchange?username=test&password=test&exchangeType=fanout"
}


class RabbitMQConsumer(master: ActorRef) extends Consumer {
  def endpointUri = "rabbitmq://localhost/testExchange?username=test&password=test&exchangeType=fanout&threadPoolSize=1"

  def receive = {
    case msg: CamelMessage =>
      Logger.debug("Received %s from rabbit MQ".format(msg.bodyAs[String]))
      val json: JsValue = Json.parse(msg.bodyAs[String])

      master ! SmsDisplay.smsDisplayFormat.reads(json).get
  }
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

    case sms @ Sms(_, _, _, _) =>
      Logger.debug(s"ReceivedSms sms $sms")
      val bytes = SmsDisplay.smsDisplayFormat.writes(SmsDisplay.fromSms(sms)).toString()
      Logger.debug(s"Sending $bytes to rabbitmq")
      SmsUpdatesMaster.rabbitMQProducer ! bytes

    case signal @ Signal(_) =>
      Logger.debug(s"Broadcast signal $signal")
      webSocketOutActors foreach {outActor => outActor ! Signal.signalFormat.writes(signal)}

    case smsDisplay @ SmsDisplay(_, _, _, _) =>
      Logger.debug(s"Broadcast smsDisplay $smsDisplay")
      webSocketOutActors foreach {outActor => outActor ! SmsDisplay.smsDisplayFormat.writes(smsDisplay)}

  }
}


object SmsUpdatesWebSocketActor {
  def props(out: ActorRef, master: ActorRef) = Props(new SmsUpdatesWebSocketActor(out, master))
}


class SmsUpdatesWebSocketActor(val outActor: ActorRef, val master: ActorRef) extends Actor {
  def receive = {
    case _ =>
  }

  override def postStop() = {
    master ! SmsUpdatesMaster.Disconnect(outActor)
  }
}
