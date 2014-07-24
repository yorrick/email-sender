package ems.backend

import scala.util.{Try, Failure, Success}
import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.ClassTag

import org.joda.time.DateTime
import securesocial.core.{AuthenticationMethod, BasicProfile}
import akka.util.ByteString
import redis.ByteStringFormatter
import securesocial.core.services.CacheService
import securesocial.core.authenticator.{CookieAuthenticator, Authenticator, AuthenticatorStore}

import play.modules.rediscala.RedisPlugin
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import ems.models.User


/**
 * An distributed AuthenticationStore based on rediscala async client (using play2-rediscala plugin)
 */
abstract class RedisAuthenticatorStore[A <: Authenticator[_]](cacheService: CacheService) extends AuthenticatorStore[A] {

  implicit val system = Akka.system
  val redisClient = RedisPlugin.client()

  implicit def byteStringFormatter: ByteStringFormatter[A]

  /**
   * Allows for future result logging
   * @param msg
   * @return
   */
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

    redisClient.get[A](id) andThen logResult(s"REDIS: find for id $id")
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

    redisClient.set(authenticator.id, authenticator) andThen logResult(s"REDIS: save authenticator $authenticator")

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


/**
 * Implementation for CookieAuthenticator
 * @param cacheService
 */
class RedisCookieAuthenticatorStore(cacheService: CacheService)
    extends RedisAuthenticatorStore[CookieAuthenticator[User]](cacheService) {
  override val byteStringFormatter = new CookieAuthenticatorFormatter(this)
}


class CookieAuthenticatorFormatter(val store: AuthenticatorStore[CookieAuthenticator[User]])
    extends ByteStringFormatter[CookieAuthenticator[User]] {

  val encoding = "UTF-8"

  /**
   * Work around to avoid serializing parameterized type with macros
   * See http://stackoverflow.com/questions/20134595/automatic-serialization-deserialization-of-generic-case-classes-to-from-json-in
   * if someday we want to make it work
   */
  case class SerializableCookieAuthenticator(id: String, user: User, expirationDate: DateTime,
                                             lastUsed: DateTime, creationDate: DateTime)

  object SerializableCookieAuthenticator {
    def fromCookieAuthenticator(cookieAuth: CookieAuthenticator[User]) = SerializableCookieAuthenticator(
      cookieAuth.id, cookieAuth.user, cookieAuth.expirationDate, cookieAuth.lastUsed, cookieAuth.creationDate
    )

    def toCookieAuthenticator(scAuth: SerializableCookieAuthenticator) = new CookieAuthenticator(
      scAuth.id, scAuth.user, scAuth.expirationDate, scAuth.lastUsed, scAuth.creationDate, store
    )
  }

  import ems.models.User._
  implicit val format: Format[SerializableCookieAuthenticator] = Json.format[SerializableCookieAuthenticator]

  def serialize(cAuth: CookieAuthenticator[User]): ByteString = {
    val serializableCookieAuthenticator = SerializableCookieAuthenticator.fromCookieAuthenticator(cAuth)
    val stringAuth = Json.stringify(format.writes(serializableCookieAuthenticator))
    ByteString(stringAuth.getBytes(encoding))
  }

  def deserialize(bs: ByteString): CookieAuthenticator[User] = {
    val byteArray: Array[Byte] = bs.toArray
    val stringAuth = new String(byteArray, encoding)

    val jsResult: JsResult[CookieAuthenticator[User]] = Json.parse(stringAuth).validate[SerializableCookieAuthenticator] map {
      SerializableCookieAuthenticator.toCookieAuthenticator(_)
    }

    jsResult.get

//    val result = bs.utf8String.split('|').toList
//    val emptyProfile = BasicProfile("", "", None, None, None, None, None, AuthenticationMethod.OAuth2, None, None, None)
//    new CookieAuthenticator("", User(emptyProfile), DateTime.now(), DateTime.now(), DateTime.now(), store)
  }
}


///**
// * Implementation for HttpHeaderAuthenticator
// * @param cacheService
// */
//class RedisHttpHeaderAuthenticatorStore(cacheService: CacheService) extends RedisAuthenticatorStore[HttpHeaderAuthenticator[User]](cacheService) {
//
//  override val byteStringFormatter = new ByteStringFormatter[HttpHeaderAuthenticator[User]] {
//    def serialize(smsDisplay: HttpHeaderAuthenticator[User]): ByteString = {
//      ByteString()
//    }
//
//    def deserialize(bs: ByteString): HttpHeaderAuthenticator[User] = {
//      val result = bs.utf8String.split('|').toList
//
//      //      id: String, user: U, expirationDate: DateTime,
//      //      lastUsed: DateTime,
//      //      creationDate: DateTime,
//      //      @transient
//      //      store: AuthenticatorStore[CookieAuthenticator[U]]
//
//      new HttpHeaderAuthenticator()
//    }
//  }
//
//}