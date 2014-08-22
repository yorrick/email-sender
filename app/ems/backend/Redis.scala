package ems.backend


import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import akka.pattern.gracefulStop
import play.api.Logger
import play.libs.Akka
import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import redis.api.pubsub.Message
import redis.{RedisPubSub, RedisClient}
import scaldi.{Injector, Injectable}


trait RedisService {
  /**
   * Returns a client to interoperate with redis
   * @return
   */
  def client: RedisClient

  /**
   * Blocks until connections are open
   */
  def openConnections: Unit

  /**
   * Blocks until connections are closed
   */
  def closeConnections: Unit
}

/**
 * Simple redis service: one client connection, one subscriber allowed
 */
class DefaultRedisService(implicit inj: Injector) extends RedisService with Injectable {

  // since this service is injected at startup by scaldi Module, we cannot use scaldi's play config injection...
  val channels: Seq[String] = Seq(current.configuration.getString("notifications.redis.channel").get)
  val onMessage: Message => Unit = inject[AkkaServices].onMessage

  implicit val system = Akka.system

  private var internalClient: Option[RedisClient] = None
  private var internalPubSub: Option[RedisPubSub] = None

  def client = internalClient.get

  def subscribe(channels: Seq[String], onMessage: Message => Unit) =
    RedisPlugin.pubsub(channels = channels, patterns = Seq[String](), onMessage = onMessage)


  def openConnections {
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
