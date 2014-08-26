package ems.backend.updates

import akka.actor.ActorRef
import ems.backend.utils.{RedisService, LogUtils}
import ems.models.{Forwarding, ForwardingDisplay, Signal}
import play.api.Logger
import play.api.Play._
import scaldi.{Injectable, Injector}

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}

/**
 * This actor stores all websocket connections to browsers.
 * Each user can have multiple connections at the same time.
 */
class WebsocketUpdatesServiceActor(implicit inj: Injector) extends UpdatesServiceActor with LogUtils with Injectable {

  val redisService = inject[RedisService]
  implicit val executionContext = inject[ExecutionContext]
  val redisChannel = current.configuration.getString("ems.backend.utils.DefaultRedisService.channel").get
  // since this service is injected at startup by scaldi Module, we cannot use scaldi's play config injection...
  val channels: Seq[String] = Seq(redisChannel)

  val logger: Logger = Logger("application.WebsocketUpdatesServiceActor")

  /**
   * userId -> list of websocket connections
   */
  private val webSocketOutActors = mutable.Map[String, mutable.ListBuffer[ActorRef]]()

  def receive = {
    case Connect(user, actor) =>
      logger.debug("Opened a websocket connection")

      webSocketOutActors.get(user.id) match {
        case Some(websocketOutList) => websocketOutList += actor
        case None => webSocketOutActors(user.id) = mutable.ListBuffer(actor)
      }

      sender ! webSocketOutActors.toMap

    case Disconnect(user, actor) =>
      logger.debug("Websocket connection has closed")

      webSocketOutActors.get(user.id) match {
        case Some(websocketOutList) => websocketOutList -= actor
        case None =>
      }

      webSocketOutActors.remove(user.id)
      sender ! webSocketOutActors.toMap

    case forwarding: Forwarding =>
      logger.debug(s"Sending message to redis pubsub $forwarding")
      // send notification to redis
      val pub = redisService.client.publish(redisChannel, ForwardingDisplay.fromForwarding(forwarding))

      val senderRef = sender()

      // send back whatever result we've got
      pub onComplete {
        case result @ _ => senderRef ! result
      }

    case signal: Signal =>
      webSocketOutActors.values.flatMap(identity) foreach {_ ! Signal.signalFormat.writes(signal)}
      sender ! true

    case forwardingDisplay: ForwardingDisplay =>
      logger.debug(s"Broadcast forwardingDisplay $forwardingDisplay")

      webSocketOutActors.get(forwardingDisplay.userId) map {
        _ foreach { outActor =>
          val json = ForwardingDisplay.forwardingDisplayFormat.writes(forwardingDisplay)
          outActor ! json
        }
      }

      sender ! true
  }

}
