package ems.controllers

import scala.concurrent.duration._

import akka.util.Timeout

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

import ems.backend.MongoDB
import ems.backend.WebsocketInputActor
import ems.models._


/**
 * Handles all http requests from browsers
 */
object SmsController extends Controller {

  /**
   * GET for browser
   * @return
   */
  def list = Action.async {
    import scala.language.postfixOps
    implicit val timeout = Timeout(1 second)

    val futureSmsList = MongoDB.listSms().mapTo[List[Sms]]

    futureSmsList.map(smsList => Ok(ems.views.html.sms.list(smsList map {SmsDisplay.fromSms(_)}))) recover {
      case error @ _ =>
        val message = s"Could not get sms list: $error"
        Logger.warn(message)
        NotFound(message)
    }
  }

  /**
   * Initiate the websocket connection
   */
  def updatesSocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => outActor =>
    WebsocketInputActor(outActor)
  }

}
