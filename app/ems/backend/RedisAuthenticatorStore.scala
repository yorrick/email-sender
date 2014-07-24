package ems.backend


import securesocial.core.services.CacheService

import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.ClassTag

import securesocial.core.authenticator.{Authenticator, AuthenticatorStore}

import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Try, Failure, Success}


/**
 * An distributed AuthenticationStore based on rediscala async client (using play2-rediscala plugin)
 */
class RedisAuthenticatorStore[A <: Authenticator[_]](cacheService: CacheService) extends AuthenticatorStore[A] {

  implicit val system = Akka.system
  val redisClient = RedisPlugin.client()

  def logResult(msg: String): PartialFunction[Try[_], Unit] = {
    case Success(result) => println(s"$msg: SUCCESS $result")
    case Failure(e) => println(s"$msg: ERROR $e")
  }

  /**
   * Retrieves an Authenticator from the cache
   *
   * @param id the authenticator id
   * @param ct the class tag for the Authenticator type
   * @return an optional future Authenticator
   */
  override def find(id: String)(implicit ct: ClassTag[A]): Future[Option[A]] = {
    Logger.debug(s"Find authenticator with id $id")

    redisClient.get[String](id) andThen logResult(s"REDIS: find for id $id")
    cacheService.getAs[A](id)(ct) andThen logResult(s"EHCACHE: find for id $id")
  }

  /**
   * Saves/updates an authenticator into the cache
   *
   * @param authenticator the istance to save
   * @param timeoutInSeconds the timeout.
   * @return the saved authenticator
   */
  override def save(authenticator: A, timeoutInSeconds: Int): Future[A] = {
    Logger.debug(s"Save authenticator $authenticator")

    redisClient.set(authenticator.id, "dumbvalue") andThen logResult(s"REDIS: save authenticator $authenticator")

    import ExecutionContext.Implicits.global
    cacheService.set(authenticator.id, authenticator, timeoutInSeconds).map { _ => authenticator }  andThen logResult(s"EHCACHE: save authenticator $authenticator")
  }

  /**
   * Deletes an Authenticator from the cache
   *
   * @param id the authenticator id
   * @return a future of Unit
   */
  override def delete(id: String): Future[Unit] ={
    Logger.debug(s"Delete authenticator with id $id")

    redisClient.del(id) andThen logResult(s"REDIS: del for id $id")
    cacheService.remove(id) andThen logResult(s"EHCACHE: del for id $id")
  }
}
