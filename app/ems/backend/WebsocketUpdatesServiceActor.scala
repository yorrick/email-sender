package ems.backend

import play.api.Play._

import scala.collection.mutable
import scala.util.{Try, Success}

import scaldi.akka.AkkaInjectable
import scaldi.{Injector, Injectable}
import akka.actor.{ActorSystem, Actor, ActorRef}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import ems.models._
import ems.backend.utils.LogUtils


/**
 * A trait that eases access to UpdatesServiceActor
 */
trait WithUpdateService extends AkkaInjectable {

  implicit val inj: Injector
  implicit val system: ActorSystem
  def updatesServiceActor: ActorRef = injectActorRef[UpdatesServiceActor]

  /**
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Forwarding], Unit] = {
    case Success(forwarding) =>
      updatesServiceActor ! forwarding
  }

}


/**
 * An actor that pushes notifications to clients
 */
trait UpdatesServiceActor extends Actor


case class Connect(user: User, outActor: ActorRef)
case class Disconnect(user: User, outActor: ActorRef)


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
