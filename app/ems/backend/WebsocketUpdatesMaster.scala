package ems.backend

import java.net.InetSocketAddress

import scala.collection.mutable
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._

import akka.actor.{Actor, Props, ActorRef}
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import ems.models._


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

  // we have to parse application config to create the subscriber
  val redisConfig = RedisPlugin.parseConf(current.configuration)
  val address = new InetSocketAddress(redisConfig._1, redisConfig._2)
  val authPassword = redisConfig._3 map {userPasswordTuple => userPasswordTuple._2}

  // create redis subscriber instance (the dispatcher is provided by the rediscala lib)
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
