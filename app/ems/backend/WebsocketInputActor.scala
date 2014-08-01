package ems.backend

import akka.actor.{Actor, ActorRef, Props}
import ems.models.User


/**
 * Allows easy Props creation
 */
object WebsocketInputActor {
  def apply(user: User, outActor: ActorRef) = Props(classOf[WebsocketInputActor], user, outActor)
}


/**
 * Actor given to play to handle websocket input
 * @param outActor
 */
class WebsocketInputActor(val user: User, val outActor: ActorRef) extends Actor {

  import ems.backend.WebsocketUpdatesMaster._

  /**
   * For now we do not expect anything from the browsers
   * @return
   */
  def receive = {
    case _ =>
  }

  override def preStart() = {
    websocketUpdatesMaster ! Connect(user, outActor)
  }

  override def postStop() = {
    websocketUpdatesMaster ! Disconnect(user, outActor)
  }
}


