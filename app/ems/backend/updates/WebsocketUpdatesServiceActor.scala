package ems.backend.updates

import akka.actor.ActorRef
import ems.backend.utils.{RedisService, LogUtils}
import ems.models.{Forwarding, ForwardingDisplay, Signal}
import play.api.Logger
import play.api.Play._
import play.api.libs.concurrent.Execution.Implicits._
import scaldi.{Injectable, Injector}

import scala.collection.mutable

/**
 * This actor stores all websocket connections to browsers.
 * Each user can have multiple connections at the same time.
 */
class WebsocketUpdatesServiceActor(implicit inj: Injector) extends UpdatesServiceActor with LogUtils with Injectable {

  val redisService = inject[RedisService]
  // since this service is injected at startup by scaldi Module, we cannot use scaldi's play config injection...
  val channels: Seq[String] = Seq(current.configuration.getString("notifications.redis.channel").get)
  val redisChannel = "forwardingList"

  /**
   * userId -> list of websocket connections
   */
  private val webSocketOutActors = mutable.Map[String, mutable.ListBuffer[ActorRef]]()

  def receive = {
    case Connect(user, actor) =>
      Logger.debug("Opened a websocket connection")

      webSocketOutActors.get(user.id) match {
        case Some(websocketOutList) => websocketOutList += actor
        case None => webSocketOutActors(user.id) = mutable.ListBuffer(actor)
      }

      Logger.debug(s"webSocketOutActors: $webSocketOutActors")

      sender ! webSocketOutActors.toMap

    case Disconnect(user, actor) =>
      Logger.debug("Websocket connection has closed")

      webSocketOutActors.get(user.id) match {
        case Some(websocketOutList) => websocketOutList -= actor
        case None =>
      }

      webSocketOutActors.remove(user.id)
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")

      sender ! webSocketOutActors.toMap

    case forwarding: Forwarding =>
      // send notification to redis
      redisService.client.publish(redisChannel, ForwardingDisplay.fromForwarding(forwarding)) andThen logResult(s"Publish forwarding $forwarding in redis")

    case signal: Signal =>
      webSocketOutActors.values.flatMap(identity) foreach {
        _ ! Signal.signalFormat.writes(signal)}

    case forwardingDisplay: ForwardingDisplay =>
      Logger.debug(s"Broadcast forwardingDisplay $forwardingDisplay")

      webSocketOutActors.get(forwardingDisplay.userId) map {
        _ foreach { outActor => outActor ! ForwardingDisplay.forwardingDisplayFormat.writes(forwardingDisplay)}
      }
  }

}
