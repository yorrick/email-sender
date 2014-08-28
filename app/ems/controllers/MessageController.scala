package ems.controllers


import akka.actor.{Props}
import ems.backend.persistence.{MessageStore, UserInfoStore}
import ems.backend.updates.WebsocketInputActor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.util.Timeout
import securesocial.core.{RuntimeEnvironment, SecureSocial}

import play.api.mvc.WebSocket
import play.api.Logger
import play.api.libs.json._
import play.api.Play.current
import scaldi.{Injectable, Injector}

import ems.models._


/**
 * Handles all http requests from browsers
 */
class MessageController(implicit inj: Injector) extends SecureSocial[User] with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]
  val messageStore = inject[MessageStore]
  val userInfoStore = inject[UserInfoStore]
  implicit val executionContext = inject[ExecutionContext]

  /**
   * GET for browser
   * @return
   */
  def list = SecuredAction.async { implicit request =>
    import scala.language.postfixOps
    implicit val timeout = Timeout(1 second)
    val user = request.user

    val result = for {
      userInfo <- userInfoStore.findUserInfoByUserId(user.id)
      messageList <- messageStore.listMessage(request.user.id).mapTo[List[Message]]
    } yield {
      val messageDisplayList = messageList map {MessageDisplay.fromMessage(_)}
      Ok(ems.views.html.message.list(messageDisplayList, user, userInfo))
    }

    result recover {
      case error @ _ =>
        val message = s"Could not get message list: $error"
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
