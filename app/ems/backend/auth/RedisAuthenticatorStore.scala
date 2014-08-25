package ems.backend.auth

import ems.backend.utils.LogUtils
import play.api.Logger
import redis.{ByteStringFormatter, RedisClient}
import securesocial.core.authenticator.{Authenticator, AuthenticatorStore}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
 * An distributed AuthenticationStore based on rediscala async client (using play2-rediscala plugin)
 * For each connected user, a redis key value entry is added.
 * The entry is dropped during logout.
 */
abstract class RedisAuthenticatorStore[A <: Authenticator[_]] extends AuthenticatorStore[A] with LogUtils {
  val logger: Logger = Logger("application.RedisAuthenticatorStore")

  implicit val byteStringFormatter: ByteStringFormatter[A]
  implicit val executionContext: ExecutionContext

  def logResult(msg: String) = super.logResult(msg, logger)

  def redisClient: RedisClient

  /**
   * Retrieves an Authenticator from the cache
   *
   * @param id the authenticator id
   * @param ct the class tag for the Authenticator type
   * @return an optional future Authenticator
   */
  override def find(id: String)(implicit ct: ClassTag[A]): Future[Option[A]] = {
    redisClient.get[A](id) andThen logResult(s"REDIS: find for id $id")
  }

  /**
   * Saves/updates an authenticator into the cache
   *
   * @param authenticator the instance to save
   * @param timeoutInSeconds the timeout.
   * @return the saved authenticator
   */
  override def save(authenticator: A, timeoutInSeconds: Int): Future[A] = {
    redisClient.set(authenticator.id, authenticator) andThen logResult(s"REDIS: save authenticator: $authenticator") map { _ => authenticator}
  }

  /**
   * Deletes an Authenticator from the cache
   *
   * @param id the authenticator id
   * @return a future of Unit
   */
  override def delete(id: String): Future[Unit] ={
    redisClient.del(id) andThen logResult(s"REDIS: del for id $id") map { _ => Unit}
  }
}
