package ems.controllers

import scala.concurrent.duration._

import akka.util.Timeout
import securesocial.core.{RuntimeEnvironment, SecureSocial}

import play.api.mvc.WebSocket
import play.api.Logger
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

import ems.backend.{SmsStore, WebsocketInputActor}
import ems.models._


/**
 * Handles all http requests from browsers
 */
class SmsController(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {

  /**
   * GET for browser
   * @return
   */
  def list = SecuredAction.async { implicit request =>
    import scala.language.postfixOps
    implicit val timeout = Timeout(1 second)
    implicit val user = Some(request.user)

    val futureSmsList = SmsStore.listSms(request.user._id).mapTo[List[Sms]]

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
   */
  def updatesSocket = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    SecureSocial.currentUser[User] map {
      case Some(user) => Right { outActor =>
        WebsocketInputActor(user, outActor)
      }
      case None => Left(Forbidden)
    }
  }

}
