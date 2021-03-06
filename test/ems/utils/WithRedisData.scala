package ems.utils

import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._

import redis.RedisClient
import akka.util.ByteString
import akka.pattern.gracefulStop
import org.specs2.execute.{Result, AsResult}
import play.modules.rediscala.RedisPlugin
import play.api.test._
import play.api.Logger
import play.libs.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
 * Context for redis based tests.
 * Data must be given as ByteString, this way serialization is handled by the test.
 * This context uses a dedicated redis connection to initialize data, and closes it once it's done.
 * @param app
 * @param data
 */
abstract class WithRedisData(data: => Seq[(String, ByteString)] = Seq(),
                             override val app: FakeApplication = FakeApplication()) extends WithApplication(app) {


  lazy val closeRedisConnectionsTimeout = 5.second
  lazy val flushDBTimeout = 5.second

  override def around[T: AsResult](t: => T): Result = super.around {
    initRedis
    t
  }

  /**
   * Initializes redis data
   */
  def initRedis {
    implicit val system = Akka.system
    implicit val client = RedisPlugin.client()

    try {
      Logger.debug("WithRedisData: initializing redis data")

      val resultsFuture: Future[Seq[Boolean]] = flushDB flatMap { _ => setAllData}
      val results: Seq[Boolean] = Await.result(resultsFuture, flushDBTimeout)

      if (results exists {_ == false}) {
        throw new Exception("WithRedisData: Could not initialize all redis data")
      } else {
        Logger.debug("WithRedisData: initialized redis data")
      }
    } finally {
      client.stop()
      val stoppedFuture: Future[Boolean] = gracefulStop(client.redisConnection, closeRedisConnectionsTimeout)
      Logger.debug("WithRedisData: Closing redis connection")
      val stopped = Await.result(stoppedFuture, closeRedisConnectionsTimeout)
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
