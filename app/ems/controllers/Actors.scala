package ems.controllers

import java.net.InetSocketAddress

import akka.pattern

import scala.collection.mutable
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._

import akka.actor.{Actor, Props, ActorRef}
import akka.util.ByteString
import redis.api.pubsub.{PMessage, Message}
import redis.actors.RedisSubscriberActor
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import ems.models._


/**
 * Instance of the actor that handle sms forwarding
 */
object SmsForwarder {
  val smsForwarder = Akka.system.actorOf(Props[SmsForwarder], name="smsForwarder")
}


/**
 * Handles sms forwarding logic
 */
class SmsForwarder extends Actor {
  import ems.controllers.WebsocketUpdatesMaster.notifyWebsockets
  import ems.controllers.MongoDB._
  import ems.controllers.Mailgun._

  def receive = {
    case sms: Sms =>
      for {
        sms <- save(sms) andThen notifyWebsockets
        sms <- pattern.after(2.second, Akka.system.scheduler)(sendEmail(sms))
        sms <- updateStatusById(sms) andThen notifyWebsockets
      } yield sms

    case MailgunEvent(messageId, DELIVERED) =>
      setStatusByMailgunId(messageId, AckedByMailgun) andThen notifyWebsockets

    case MailgunEvent(messageId, _) =>
      setStatusByMailgunId(messageId, FailedByMailgun) andThen notifyWebsockets
  }
}


/**
 * Instance of the actor used to push messages to browsers
 */
object WebsocketUpdatesMaster {

  case class Connect(val outActor: ActorRef)
  case class Disconnect(val outActor: ActorRef)

  // create the master actor once
  val websocketUpdatesMaster = Akka.system.actorOf(Props[WebsocketUpdatesMaster], name="websocketUpdatesMaster")

  implicit val system = Akka.system
  val redisClient = RedisPlugin.client()
  val redisChannel = "smsList"

  // use application configuration
  val redisConfig = RedisPlugin.parseConf(current.configuration)
  val address = new InetSocketAddress(redisConfig._1, redisConfig._2)
  val authPassword = redisConfig._3 map {userPasswordTuple => userPasswordTuple._2}

  // create redis subscriber instance
  // TODO configure dispatcher
  Akka.system.actorOf(Props(classOf[RedisActor], address, Seq(redisChannel), Seq(), authPassword)
    .withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

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
}


/**
 * This actor stores all websocket connections to browsers
 */
class WebsocketUpdatesMaster extends Actor {
  import WebsocketUpdatesMaster._

  /** List of output websocket actors that are connected to the node */
  private val webSocketOutActors = mutable.ListBuffer[ActorRef]()

  def receive = {
    case Connect(actor) =>
      Logger.debug("Opened a websocket connection")
      webSocketOutActors += actor
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")

    case Disconnect(actor) =>
      Logger.debug("Websocket connection has closed")
      webSocketOutActors -= actor
      Logger.debug(s"webSocketOutActors: $webSocketOutActors")

    case sms: Sms =>
      Logger.debug(s"ReceivedSms sms $sms")

      // send notification to redis
      WebsocketUpdatesMaster.redisClient.publish(WebsocketUpdatesMaster.redisChannel, SmsDisplay.fromSms(sms)) onComplete {
        case Success(message) => Logger.info(s"Successfuly published message ($message)")
        case Failure(t) => Logger.warn("An error has occured: " + t.getMessage)
      }

    case signal: Signal =>
      //      Logger.debug(s"Broadcast signal $signal")
      webSocketOutActors foreach {outActor => outActor ! Signal.signalFormat.writes(signal)}

    case smsDisplay: SmsDisplay =>
      Logger.debug(s"Broadcast smsDisplay $smsDisplay")
      webSocketOutActors foreach {outActor => outActor ! SmsDisplay.smsDisplayFormat.writes(smsDisplay)}

  }
}


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

  import WebsocketUpdatesMaster._

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


/**
 * This listener consumes messages from redis, and give them to the websocketUpdatesMaster
 * @param master
 * @param channels
 */
class RedisActor(address: InetSocketAddress, channels: Seq[String], patterns: Seq[String], authPassword: Option[String])
  extends RedisSubscriberActor(address, channels, patterns, authPassword) {

  import WebsocketUpdatesMaster._

  def onMessage(message: Message) {
    Logger.debug(s"message received: $message")
    val smsDisplay = SmsDisplay.smsDisplayByteStringFormatter.deserialize(ByteString(message.data))
    websocketUpdatesMaster ! smsDisplay
  }

  def onPMessage(pmessage: PMessage) {
    Logger.debug(s"pmessage received: $pmessage")
  }
}


