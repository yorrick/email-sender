package ems.controllers


import akka.actor.{Props, ActorRef}
import scaldi.akka.AkkaInjectable

import scala.concurrent.duration._

import akka.util.Timeout
import securesocial.core.{RuntimeEnvironment, SecureSocial}

import play.api.mvc.WebSocket
import play.api.Logger
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scaldi.{Injectable, Injector}

import ems.backend.{UserInfoStore, ForwardingStore, WebsocketInputActor}
import ems.models._


/**
 * Handles all http requests from browsers
 */
class ForwardingController(implicit inj: Injector) extends SecureSocial[User] with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]
  val forwardingStore = inject[ForwardingStore]

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
      forwardingList <- forwardingStore.listForwarding(request.user.id).mapTo[List[Forwarding]]
    } yield {
      val forwardingDisplayList = forwardingList map {ForwardingDisplay.fromForwarding(_)}
      Ok(ems.views.html.forwarding.list(forwardingDisplayList, user, userInfo))
    }

    result recover {
      case error @ _ =>
        val message = s"Could not get forwarding list: $error"
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
        // here we create props by hand, since scaldi does not support custom parameters injection
        Props(classOf[WebsocketInputActor], user, outActor, inj)
      }
      case None => Left(Forbidden)
    }
  }

}
