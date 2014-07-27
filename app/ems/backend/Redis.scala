package ems.backend

import play.libs.Akka
import play.modules.rediscala.RedisPlugin
import play.api.Play.current

/**
 * Store redis client connections
 */
object Redis {

  implicit val system = Akka.system
  /**
   * Single redis client for all application
   */
  val redisClient = RedisPlugin.client()
  val redisPubsub = RedisPlugin.pubsub(channels = Seq(WebsocketUpdatesMaster.redisChannel),
      patterns = Seq[String](), onMessage = WebsocketUpdatesMaster.onMessage)
}
