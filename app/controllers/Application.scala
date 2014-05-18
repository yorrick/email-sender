package controllers

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.libs.Akka
import akka.actor._


case class Sms(val from: String, val content: String)
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

  // TODO write a body parser
  def sms = Action { request =>
  	Logger.debug(s"Got this request ${request}")
  	Logger.debug(s"Got those data ${request.body}")
  	
  	// TODO build sms object with a Form to check data validity
  	val sms = Sms("06123456789", "Toto")
  	
	val smsStorage = Akka.system.actorOf(Props[SmsStorage])
	smsStorage ! StoreSms(sms)

	// TODO answer with TwiML instead
    Ok("Hello there!")
  }

}