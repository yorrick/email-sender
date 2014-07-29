package ems.backend

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import akka.pattern.gracefulStop
import play.api.Logger
import play.libs.Akka
import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Store redis client connections
 */
object Redis {
  private var instanceOption: Option[Redis] = None

  /**
   * Returns an instance that manages the connections
   * @return
   */
  def instance = instanceOption.get

  /**
   * Opens connections to redis
   */
  def openConnections {
    instanceOption = Some(new Redis())
  }

  /**
   * Blocks until connections are closed
   */
  def closeConnections {
    instanceOption map { _.closeConnections() }
  }
}

class Redis {
  implicit val system = Akka.system

  Logger.debug("Creating application's redis connections")

  /**
   * Single redis client for application
   */
  val redisClient = RedisPlugin.client()

  /**
   * Single pub sub client for application
   */
  val redisPubsub = RedisPlugin.pubsub(channels = Seq(WebsocketUpdatesMaster.redisChannel),
      patterns = Seq[String](), onMessage = WebsocketUpdatesMaster.onMessage)

  /**
   * Blocks until connections are closed
   */
  def closeConnections() {
    // wait until redisconnection actor has shut down
    redisClient.stop()
    val stoppedClient: Future[Boolean] = gracefulStop(redisClient.redisConnection, 2.second)

    redisPubsub.stop()
    val stoppedPubsub: Future[Boolean] = gracefulStop(redisPubsub.redisConnection, 2.second)

    Await.result(stoppedClient.flatMap( _ => stoppedPubsub), 1.seconds)
    Logger.info("Closed application's redis connections...")
  }
}
