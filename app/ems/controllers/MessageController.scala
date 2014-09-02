package ems.controllers


import akka.actor.{Props}
import ems.backend.persistence.{MessageStore, UserInfoStore}
import ems.backend.updates.WebsocketInputActor
import ems.controllers.utils.{ControllerUtils}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.util.Timeout
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import play.api.mvc.WebSocket
import play.api.Logger
import play.api.libs.json._
import play.api.Play.current
import scaldi.{Injectable, Injector}
import scala.language.postfixOps
import ems.models._


/**
 * Handles all http requests from browsers
 */
class MessageController(implicit val injector: Injector) extends ControllerUtils with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]
  val messageStore = inject[MessageStore]
  val userInfoStore = inject[UserInfoStore]
  implicit val executionContext = inject[ExecutionContext]

  /**
   * GET for browser
   * @return
   */
  def list = securedContextAction("footer") { implicit user => implicit ctx => implicit request =>
    implicit val timeout = Timeout(1 second)

    val result = for {
      userInfo <- userInfoStore.findUserInfoByUserId(user.id)
      messageList <- messageStore.listMessage(user.id).mapTo[List[Message]]
    } yield {
      val messageDisplayList = messageList map {
        MessageDisplay.fromMessage(_)
      }
      Ok(ems.views.html.message.list(messageDisplayList, user, userInfo))
    }

    result recover {
      case error@_ =>
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
        Props(classOf[WebsocketInputActor], user, outActor, injector)
      }
      case None => Left(Forbidden)
    }
  }

}
