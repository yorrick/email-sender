package ems.controllers

import scala.concurrent.duration._

import akka.util.Timeout
import securesocial.core.{RuntimeEnvironment, SecureSocial}

import play.api.mvc.WebSocket
import play.api.Logger
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

import ems.backend.{UserInfoStore, SmsStore, WebsocketInputActor}
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
    val user = request.user

    val result = for {
      userInfo <- UserInfoStore.findUserInfoByUserId(user.id)
      smsList <- SmsStore.listSms(request.user.id).mapTo[List[Forwarding]]
    } yield {
      val smsDisplayList = smsList map {ForwardingDisplay.fromForwarding(_)}
      Ok(ems.views.html.sms.list(smsDisplayList, user, userInfo))
    }

    result recover {
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
