package ems.controllers


import scala.concurrent.duration._

import akka.util.Timeout
import securesocial.core.{BasicProfile, RuntimeEnvironment, SecureSocial}

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.mvc.Http.RequestHeader

import ems.backend.{DemoUser, MongoDB, WebsocketInputActor}
import ems.models._


/**
 * Handles all http requests from browsers
 */
class SmsController(override implicit val env: RuntimeEnvironment[DemoUser]) extends SecureSocial[DemoUser] {

  /**
   * GET for browser
   * @return
   */
  def list = SecuredAction.async { implicit request =>
    import scala.language.postfixOps
    implicit val timeout = Timeout(1 second)
    implicit val user = request.user.main

    val futureSmsList = MongoDB.listSms().mapTo[List[Sms]]

    futureSmsList map { smsList =>
      Ok(ems.views.html.sms.list(smsList map {SmsDisplay.fromSms(_)}))
    } recover {
      case error @ _ =>
        val message = s"Could not get sms list: $error"
        Logger.warn(message)
        NotFound(message)
    }
  }

  /**
   * Initiate the websocket connection
   * TODO authenticate the socket as well
   */
  def updatesSocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => outActor =>
    WebsocketInputActor(outActor)
  }

}
