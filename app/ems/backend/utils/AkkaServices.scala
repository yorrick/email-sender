package ems.backend.utils

import redis.api.pubsub.Message

/**
 * Provides akka related callbacks
 */
trait AkkaServices {
  def onRedisMessage(message: Message): Unit
  def scheduleAkkaEvents: Unit
}
