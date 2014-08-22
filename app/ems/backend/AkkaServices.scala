package ems.backend

import akka.actor.{ActorSystem, ActorRef}
import akka.util.ByteString
import ems.models.ForwardingDisplay
import play.api.Logger
import redis.api.pubsub.Message
import scaldi.Injector
import scaldi.akka.AkkaInjectable


/**
 * Stores the akka related callbacks
 */
class AkkaServices(implicit inj: Injector) extends AkkaInjectable {
//object AkkaServices extends AkkaInjectable {

  implicit val system = inject[ActorSystem]
  val updatesServiceActor: ActorRef = injectActorRef[UpdatesServiceActor]

  /**
   * Consumes messages from redis, and give them to the websocketUpdatesMaster
   * @param message
   */
  def onMessage(message: Message) {
    Logger.debug(s"message received: $message")
    val forwardingDisplay = ForwardingDisplay.forwardingDisplayByteStringFormatter.deserialize(ByteString(message.data))
    updatesServiceActor ! forwardingDisplay
  }

}
