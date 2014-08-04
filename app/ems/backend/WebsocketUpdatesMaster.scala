package ems.backend

import akka.util.ByteString
import redis.api.pubsub.Message

import scala.collection.mutable
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._

import akka.actor.{Actor, Props, ActorRef}
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import ems.models._


/**
 * Instance of the actor used to push messages to browsers
 */
object WebsocketUpdatesMaster {

  case class Connect(user: User, outActor: ActorRef)
  case class Disconnect(user: User, outActor: ActorRef)

  // create the master actor once
  val websocketUpdatesMaster = Akka.system.actorOf(Props[WebsocketUpdatesMaster], name="websocketUpdatesMaster")
  val redisChannel = "smsList"

  // we periodically ping the client so the websocket connections do not close
  Akka.system.scheduler.schedule(30.second, 30.second, websocketUpdatesMaster, Ping)

  /**
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Sms], Unit] = {
    case Success(sms) =>
      websocketUpdatesMaster ! sms
  }

  /**
   * Consumes messages from redis, and give them to the websocketUpdatesMaster
   * @param message
   */
  def onMessage(message: Message) {
    Logger.debug(s"message received: $message")
    val smsDisplay = SmsDisplay.smsDisplayByteStringFormatter.deserialize(ByteString(message.data))
    websocketUpdatesMaster ! smsDisplay
  }
}


/**
 * This actor stores all websocket connections to browsers.
 * Each user can have multiple connections at the same time.
 */
class WebsocketUpdatesMaster extends Actor {
  import WebsocketUpdatesMaster._

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

    case sms: Sms =>
      // send notification to redis
      Redis.instance.redisClient.publish(WebsocketUpdatesMaster.redisChannel, SmsDisplay.fromSms(sms)) onComplete {
        case Success(message) => Logger.info(s"Successfuly published message ($message)")
        case Failure(t) => Logger.warn("An error has occured: " + t.getMessage)
      }

    case signal: Signal =>
      webSocketOutActors.values.flatMap(identity) foreach {
        _ ! Signal.signalFormat.writes(signal)}

    case smsDisplay: SmsDisplay =>
      Logger.debug(s"Broadcast smsDisplay $smsDisplay")

      webSocketOutActors.get(smsDisplay.userId) map {
        _ foreach { outActor => outActor ! SmsDisplay.smsDisplayFormat.writes(smsDisplay)}
      }
  }

}
