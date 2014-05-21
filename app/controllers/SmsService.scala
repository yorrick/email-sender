package controllers

import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.libs.Akka
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout


case class Sms(val from: String, val to: String, val content: String)
case class StoreSms(val sms: Sms)
case class ListSms()


class SmsStorage extends Actor {

  private var smsList = List[Sms]()

  def receive = {
    case StoreSms(sms) =>
      Logger.debug(s"Storing this sms: $sms")
      smsList = smsList :+ sms
    case ListSms =>
      sender ! smsList
  }

}


object SmsService extends Controller {

  val smsStorage = Akka.system.actorOf(Props[SmsStorage])

  val smsForm = Form(
	  mapping(
	    "From" -> text,
	    "To" -> text,
	    "Body" -> text
	  )(Sms.apply)(Sms.unapply))

  val emptyTwiMLResponse = """<?xml version="1.0" encoding="UTF-8"?>""" + 
	<Response>
	    <Message></Message>
	</Response>

  // GET for browser
  def list = Action.async {
  	implicit val timeout = Timeout(1 second) 
  	
	val futureSmsList = smsStorage.ask(ListSms).mapTo[List[Sms]] recover {
	  case _ =>	List[Sms]()
	}

	futureSmsList.map(smsList => Ok(views.html.sms.list(smsList)))
  }

  // POST for twilio when we receive an SMS
  def receive = Action { implicit request =>
	smsForm.bindFromRequest.fold(
	  formWithErrors => {
	  	val message = s"Could not bind the form: ${formWithErrors}"
	  	Logger.debug(message)
	    BadRequest(message)
	  },
	  sms => {
	  	Logger.debug(s"Built sms object $sms")
		smsStorage ! StoreSms(sms)

    	Ok(emptyTwiMLResponse)
	  }
	)
  }

}