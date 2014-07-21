package ems.backend

import akka.actor.{Actor, ActorRef, Props}


/**
 * Allows easy Props creation
 */
object WebsocketInputActor {
  def apply(outActor: ActorRef) = Props(classOf[WebsocketInputActor], outActor)
}


/**
 * Actor given to play to handle websocket input
 * @param outActor
 */
class WebsocketInputActor(val outActor: ActorRef) extends Actor {

  import ems.backend.WebsocketUpdatesMaster._

  /**
   * For now we do not expect anything from the browsers
   * @return
   */
  def receive = {
    case _ =>
  }

  override def preStart() = {
    websocketUpdatesMaster ! Connect(outActor)
  }

  override def postStop() = {
    websocketUpdatesMaster ! Disconnect(outActor)
  }
}


