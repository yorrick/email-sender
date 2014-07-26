package ems.backend

import java.net.InetSocketAddress

import akka.util.ByteString
import redis.api.pubsub.{PMessage, Message}
import redis.actors.RedisSubscriberActor
import play.api.Logger

import ems.models._
import ems.backend.WebsocketUpdatesMaster._


/**
 * This listener consumes messages from redis, and give them to the websocketUpdatesMaster
 * TODO use RedisPubSub instead! and integrate it to play2-rediscala
 */
class RedisActor(address: InetSocketAddress, channels: Seq[String], patterns: Seq[String], authPassword: Option[String])
  extends RedisSubscriberActor(address, channels, patterns, authPassword) {

  def onMessage(message: Message) {
    Logger.debug(s"message received: $message")
    val smsDisplay = SmsDisplay.smsDisplayByteStringFormatter.deserialize(ByteString(message.data))
    websocketUpdatesMaster ! smsDisplay
  }

  def onPMessage(pmessage: PMessage) {
    Logger.debug(s"pmessage received: $pmessage")
  }
}
