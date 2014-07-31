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
 * This actor stores all websocket connections to browsers
 */
class WebsocketUpdatesMaster extends Actor {
  import WebsocketUpdatesMaster._

  /** List of output websocket actors that are connected to the node */
  private val webSocketOutActors = mutable.Map[String, ActorRef]()

  def userOutActors(lookupUserId: String) =
    (webSocketOutActors filter { case (userId, outActor) => lookupUserId == userId }).values

  def receive = {
    case Connect(user, actor) =>
      Logger.debug("Opened a websocket connection")
      webSocketOutActors(user._id.stringify) = actor
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")

    case Disconnect(user, actor) =>
      Logger.debug("Websocket connection has closed")
      webSocketOutActors.remove(user._id.stringify)
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")

    case sms: Sms =>
      // send notification to redis
      Redis.instance.redisClient.publish(WebsocketUpdatesMaster.redisChannel, SmsDisplay.fromSms(sms)) onComplete {
        case Success(message) => Logger.info(s"Successfuly published message ($message)")
        case Failure(t) => Logger.warn("An error has occured: " + t.getMessage)
      }

    case signal: Signal =>
      webSocketOutActors.values foreach {outActor => outActor ! Signal.signalFormat.writes(signal)}

    case smsDisplay: SmsDisplay =>
      Logger.debug(s"Broadcast smsDisplay $smsDisplay")
      userOutActors(smsDisplay.userId) foreach {outActor => outActor ! SmsDisplay.smsDisplayFormat.writes(smsDisplay)}
  }
}
