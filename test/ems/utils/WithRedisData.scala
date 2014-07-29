package ems.utils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import redis.RedisClient
import akka.util.ByteString
import akka.pattern.gracefulStop
import org.specs2.execute.{Result, AsResult}
import play.modules.rediscala.RedisPlugin
import play.api.test._
import play.api.Logger
import play.libs.Akka
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Context for redis based tests
 * @param app
 * @param data
 */
abstract class WithRedisData(data: => Seq[(String, ByteString)] = Seq(),
                             override val app: FakeApplication = FakeApplication()) extends WithApplication(app) {


  lazy val timeout = 5.second

  override def around[T: AsResult](t: => T): Result = super.around {
    initRedis()
    t
  }

  /**
   * Initializes redis data
   */
  def initRedis() = {
    implicit val system = Akka.system
    implicit val client = RedisPlugin.client()

    try {
      Logger.debug("WithRedisData: initializing redis data")

      val resultsFuture: Future[Seq[Boolean]] = flushDB flatMap { _ => setAllData}
      val results: Seq[Boolean] = Await.result(resultsFuture, 2.second)

      if (results exists {_ == false}) {
        throw new Exception("Could not initialize all redis data")
      } else {
        Logger.debug("WithRedisData: initialized redis data")
      }
    } finally {
      client.stop()
      val stoppedFuture: Future[Boolean] = gracefulStop(client.redisConnection, timeout)
      Logger.debug("WithRedisData: Closing redis connection")
      val stopped = Await.result(stoppedFuture, timeout)
      Logger.debug("WithRedisData: Closed redis connection")
    }
  }

  /**
   * Flush all data in the current DB
   * @return
   */
  def flushDB(implicit client: RedisClient): Future[Boolean] = client.flushdb() filter { _ == true}

  /**
   * Inserts all data in redis
   * @return
   */
  def setAllData(implicit client: RedisClient): Future[Seq[Boolean]] = Future.sequence(data map {
    case (key, value) => client.set(key, value)
  })
}
