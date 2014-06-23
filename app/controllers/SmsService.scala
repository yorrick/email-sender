package controllers

import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.mvc.{Action, Controller}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.api.libs.json._

import play.libs.Akka

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import reactivemongo.api._


// TODO add creation date
case class Sms(val from: String, val to: String, val content: String)

object JsonFormats {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  // Generates Writes and Reads for sms thanks to Json Macros
  implicit val smsFormat = Json.format[Sms]
}


object SmsStorage {

  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection]("smslist")

  def storeSms(sms: Sms) {
    import JsonFormats._
    Logger.debug(s"Storing this sms: $sms")
    collection.insert(sms)
  }

  def listSms() = {
    import JsonFormats._
    // let's do our query
    val cursor: Cursor[Sms] = collection.
      // find all sms
      find(Json.obj()).
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
	    "Body" -> text
	  )(Sms.apply)(Sms.unapply))

  val emptyTwiMLResponse = """<?xml version="1.0" encoding="UTF-8"?>""" + 
	<Response>
	    <Message></Message>
	</Response>

  // GET for browser
  def list = Action.async {
  	import scala.language.postfixOps
  	implicit val timeout = Timeout(1 second) 
  	
    val futureSmsList = SmsStorage.listSms().mapTo[List[Sms]] recover {
      case error @ _ =>
        Logger.debug(s"Could not get sms list: $error")
        List[Sms]()
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
		  SmsStorage.storeSms(sms)

    	Ok(emptyTwiMLResponse)
	  }
	)
  }

}