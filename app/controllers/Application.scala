package controllers

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.libs.Akka
import akka.actor._
import play.api.data._
import play.api.data.Forms._


case class Sms(val from: String, val to: String, val content: String)
case class StoreSms(val sms: Sms)


class SmsStorage extends Actor {

  private var smsList = List[Sms]()

  def receive = {
    case StoreSms(sms) =>
      Logger.debug(s"Storing this sms: $sms")
      smsList = smsList :+ sms
  }
}


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Hello Play Framework"))
  }

  def sms = Action { implicit request =>
  	val smsForm = Form(
	  mapping(
	    "From" -> text,
	    "To" -> text,
	    "Body" -> text
	  )(Sms.apply)(Sms.unapply)
	)

	smsForm.bindFromRequest.fold(
	  formWithErrors => {
	  	val message = s"Could not bind the form: ${formWithErrors}"
	  	Logger.debug(message)
	    BadRequest(message)
	  },
	  sms => {
	  	Logger.debug(s"Built sms object $sms")
    	val smsStorage = Akka.system.actorOf(Props[SmsStorage])
		smsStorage ! StoreSms(sms)

		// TODO answer with TwiML instead
    	Ok("Hello there!")
	  }
	)
  }

}