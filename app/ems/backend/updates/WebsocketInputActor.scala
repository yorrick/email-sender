package ems.backend.updates

import akka.actor.{Actor, ActorRef, ActorSystem}
import ems.models.User
import scaldi.Injector
import scaldi.akka.AkkaInjectable


/**
 * Actor given to play to handle websocket input
 * @param outActor
 */
class WebsocketInputActor(val user: User, val outActor: ActorRef, implicit val inj: Injector) extends Actor with AkkaInjectable {

  implicit val system: ActorSystem = inject[ActorSystem]
  val updatesServiceActor: ActorRef = injectActorRef[UpdatesServiceActor]

  /**
   * For now we do not expect anything from the browsers
   * @return
   */
  def receive = {
    case _ =>
  }

  override def preStart() = {
    updatesServiceActor ! Connect(user, outActor)
  }

  override def postStop() = {
    updatesServiceActor ! Disconnect(user, outActor)
  }
}


