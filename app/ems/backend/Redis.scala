package ems.backend

import redis.api.pubsub.Message
import redis.{RedisPubSub, RedisClient}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import akka.pattern.gracefulStop
import play.api.Logger
import play.libs.Akka
import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._


///**
// * Store redis client connections
// */
//object Redis {
//  private var instanceOption: Option[Redis] = None
//
//  /**
//   * Returns an instance that manages the connections
//   * @return
//   */
//  def instance = instanceOption.get
//
//  /**
//   * Opens connections to redis
//   */
//  def openConnections {
//    Logger.info("Opening redis connections")
//    instanceOption = Some(new Redis())
//  }
//
//  /**
//   * Blocks until connections are closed
//   */
//  def closeConnections {
//    Logger.info("Closing redis connections")
//    instanceOption map { _.closeConnections() }
//  }
//}


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
class DefaultRedisService(channels: Seq[String], onMessage: Message => Unit) extends RedisService {
  implicit val system = Akka.system

  private var internalClient: Option[RedisClient] = None
  private var internalPubSub: Option[RedisPubSub] = None

  val client = internalClient.get

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
