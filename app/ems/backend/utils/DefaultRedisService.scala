package ems.backend.utils

import akka.pattern.gracefulStop
import play.api.Logger
import play.api.Play.current
import play.libs.Akka
import play.modules.rediscala.RedisPlugin
import redis.api.pubsub.Message
import redis.{RedisClient, RedisPubSub}
import scaldi.{Injectable, Injector}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Await, Future}


/**
 * Simple redis service: one client connection, one subscriber allowed
 */
class DefaultRedisService(implicit inj: Injector) extends RedisService with Injectable {

  // since this service is injected at startup by scaldi Module, we cannot use scaldi's play config injection...
  val channels: Seq[String] = Seq(current.configuration.getString("notifications.redis.channel").get)
  val onMessage: Message => Unit = inject[AkkaServices].onRedisMessage
  implicit val executionContext = inject[ExecutionContext]

  implicit val system = Akka.system

  private var internalClient: Option[RedisClient] = None
  private var internalPubSub: Option[RedisPubSub] = None

  def client = internalClient.get

  def subscribe(channels: Seq[String], onMessage: Message => Unit) =
    RedisPlugin.pubsub(channels = channels, patterns = Seq[String](), onMessage = onMessage)


  def openConnections {
    Logger.info("Opening application's redis connections...")
    internalClient = Some(RedisPlugin.client())
    internalPubSub = Some(RedisPlugin.pubsub(channels = channels, patterns = Seq[String](), onMessage = onMessage))
  }

  def closeConnections {
    // wait until redisconnection actor has shut down
    internalClient.get.stop()
    val stoppedClient: Future[Boolean] = gracefulStop(internalClient.get.redisConnection, 2.second)

    internalPubSub.get.stop()
    val stoppedPubsub: Future[Boolean] = gracefulStop(internalPubSub.get.redisConnection, 2.second)

    Await.result(stoppedClient.flatMap( _ => stoppedPubsub), 1.seconds)
    Logger.info("Closed application's redis connections...")
  }
}
