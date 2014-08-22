package ems.backend.utils

import scala.concurrent.duration._

import akka.actor.{ActorRef, ActorSystem}
import akka.util.ByteString
import ems.backend.updates.UpdatesServiceActor
import ems.models.{Ping, ForwardingDisplay}
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import redis.api.pubsub.Message
import scaldi.Injector
import scaldi.akka.AkkaInjectable


/**
 * Stores the akka related callbacks
 */
class DefaultAkkaServices(implicit inj: Injector) extends AkkaInjectable with AkkaServices {

  implicit val system = inject[ActorSystem]
  val updatesServiceActor: ActorRef = injectActorRef[UpdatesServiceActor]

  /**
   * Consumes messages from redis, and give them to the websocketUpdatesMaster
   * @param message
   */
  def onRedisMessage(message: Message) {
    Logger.debug(s"message received: $message")
    val forwardingDisplay = ForwardingDisplay.forwardingDisplayByteStringFormatter.deserialize(ByteString(message.data))
    updatesServiceActor ! forwardingDisplay
  }

  def scheduleAkkaEvents {
    // we periodically ping the client so the websocket connections do not close
    Akka.system.scheduler.schedule(30.second, 30.second, injectActorRef[UpdatesServiceActor], Ping)
  }

}
