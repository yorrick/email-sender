package ems.backend.utils

import redis.RedisClient

/**
 * Created by yorrick on 14-08-22.
 */
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
