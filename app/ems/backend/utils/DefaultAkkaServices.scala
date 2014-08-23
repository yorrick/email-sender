package ems.backend.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor.{ActorRef, ActorSystem}
import akka.util.ByteString
import ems.backend.updates.UpdateService
import ems.models.{Ping, ForwardingDisplay}
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current
import redis.api.pubsub.Message
import scaldi.{Injectable, Injector}


/**
 * Stores the akka related callbacks
 */
class DefaultAkkaServices(implicit inj: Injector) extends Injectable with AkkaServices {

  implicit val system = inject[ActorSystem]
  implicit val executionContext = inject[ExecutionContext]
  val updatesServiceActor: ActorRef = inject[UpdateService].updatesServiceActor

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
    Akka.system.scheduler.schedule(30.second, 30.second, updatesServiceActor, Ping)
  }

}
