package controllers

import java.net.InetSocketAddress
import controllers.SmsUpdatesMaster.{Connect, Disconnect}

import scala.collection.mutable
import scala.util.{Failure, Success}
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

import models.{Signal, Sms, SmsDisplay, Ping}


object SmsUpdatesMaster {

  /**
   * Message that must be sent to web browsers clients.
   */
  case class Connect(val outActor: ActorRef)
  case class Disconnect(val outActor: ActorRef)

  // create the master actor once
  val smsUpdatesMaster = Akka.system.actorOf(Props[SmsUpdatesMaster], name="smsUpdatesMaster")

  implicit val system = Akka.system
  val redisClient = RedisPlugin.client()
  val redisChannel = "smsList"

  // use application configuration
  val redisConfig = RedisPlugin.parseConf(current.configuration)
  val address = new InetSocketAddress(redisConfig._1, redisConfig._2)
  val authPassword = redisConfig._3 map {userPasswordTuple => userPasswordTuple._2}
  // create SubscribeActor instance
  Akka.system.actorOf(Props(classOf[SubscribeActor], smsUpdatesMaster, address, Seq(redisChannel), Seq(), authPassword)
    .withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  // we periodically ping the client so the websocket connections do not close
  Akka.system.scheduler.schedule(30.second, 30.second, smsUpdatesMaster, Ping)
}


/**
 * Consumes messages from redis
 * @param master
 * @param channels
 */
class SubscribeActor(val master: ActorRef, address: InetSocketAddress,
                     channels: Seq[String], patterns: Seq[String], authPassword: Option[String])
  extends RedisSubscriberActor(address, channels, patterns, authPassword) {

  def onMessage(message: Message) {
    Logger.debug(s"message received: $message")
    val smsDisplay = SmsDisplay.smsDisplayByteStringFormatter.deserialize(ByteString(message.data))
    master ! smsDisplay
  }

  def onPMessage(pmessage: PMessage) {
    Logger.debug(s"pmessage received: $pmessage")
  }
}


class SmsUpdatesMaster extends Actor {
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

    case sms @ Sms(_, _, _, _) =>
      Logger.debug(s"ReceivedSms sms $sms")

      // send notification to redis
      SmsUpdatesMaster.redisClient.publish(SmsUpdatesMaster.redisChannel, SmsDisplay.fromSms(sms)) onComplete {
        case Success(message) => Logger.info(s"Successfuly published message ($message)")
        case Failure(t) => Logger.warn("An error has occured: " + t.getMessage)
      }

    case signal @ Signal(_) =>
      //      Logger.debug(s"Broadcast signal $signal")
      webSocketOutActors foreach {outActor => outActor ! Signal.signalFormat.writes(signal)}

    case smsDisplay @ SmsDisplay(_, _, _, _) =>
      Logger.debug(s"Broadcast smsDisplay $smsDisplay")
      webSocketOutActors foreach {outActor => outActor ! SmsDisplay.smsDisplayFormat.writes(smsDisplay)}

  }
}


object SmsUpdatesWebSocketActor {
  def props(out: ActorRef, master: ActorRef) = Props(new SmsUpdatesWebSocketActor(out, master))
}


class SmsUpdatesWebSocketActor(val outActor: ActorRef, val master: ActorRef) extends Actor {
  def receive = {
    case _ =>
  }

  override def postStop() = {
    master ! SmsUpdatesMaster.Disconnect(outActor)
  }
}
